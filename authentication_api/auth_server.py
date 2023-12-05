#!/usr/bin/bash

import uvicorn
from fastapi import FastAPI, Response, Request, UploadFile
from fastapi.staticfiles import StaticFiles
from fastapi.responses import RedirectResponse, HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.templating import Jinja2Templates
from fastapi.middleware.httpsredirect import HTTPSRedirectMiddleware
from pydantic import BaseModel
import psycopg2
import base64
import secrets
import datetime
import threading
import json
from time import sleep

conn = psycopg2.connect(database="access_control",              # Create database connection
                        host="host.docker.internal",            #---------------------------
                        user="Crypto",
                        password="crypto",
                        port="50003")

templates = Jinja2Templates(directory="./static")               # Initialize Jinja static html dir
                                                                #---------------------------------

class Thread:                                                   # Thread that checks admin access
    def __init__(self):                                         # token timestamps and removes
        self.keep_going = True                                  # expired tokens
        t = threading.Thread(target=self.check_expirations)     #--------------------------------
        t.start()

    def check_expirations(self):
        conn2 = psycopg2.connect(database="access_control",
                        host="host.docker.internal",
                        user="Crypto",
                        password="crypto",
                        port="50003")
        cur2 = conn2.cursor()

        while True:
            if not self.keep_going:                              # Checks if main thread is running
                print("stopping thread")                         #---------------------------------
                break
            time = datetime.datetime.utcnow()
            time_now = int(datetime.datetime.strftime(time, "%y%m%d%H%M")) # reformat time
                                                                           #--------------
            cur2.execute(
                "SELECT username FROM admins WHERE expiration < %s",       # query database
                (time_now,)                                                #---------------
            )
            response_item = cur2.fetchall()
            print(response_item)

            if len(response_item) < 1:                          # Do nothing if no results
                pass                                            #-------------------------
            else:
                for obj in response_item:                       # Delete expired access token
                    username = obj[0]                           #----------------------------
                    cur2.execute(
                        "UPDATE admins SET token=NULL, expiration=NULL WHERE username=%s",
                        (username,),
                    )
                    conn2.commit()
            sleep(60)


t = Thread()                                # Initialization runs thread
                                            #---------------------------

class LoginItem(BaseModel):                 # Struct to capture json logon credentials      
    username: str = None                    #-----------------------------------------
    password: str = None


app = FastAPI()                                    # Initialize API and
app.add_middleware(HTTPSRedirectMiddleware)        # set middleware to redirect http to https
                                                   #-----------------------------------------

@app.on_event("shutdown")               # When API is shutdown, this runs
async def shutdown():                   #--------------------------------
    t.keep_going = False
    conn.close()
    conn.close()


@app.get("/")                                           # If authentication cookie is present
async def redirect(request: Request):                   #     then redirect to home page
    cookie = request.cookies.get("TMP_ACCESS")          #
    response: RedirectResponse                          # If cookie NOT present
    if cookie != None:                                  #     then redirect to login page
        response = RedirectResponse(url="/home")        #-------------------------------------
    else:
        response = RedirectResponse(url="/login")

    return response


@app.get("/login")                                      # Return login page
async def login():                                      #-------------------
    with open("./static/login.html", "r") as f:
        content = f.read()
        return HTMLResponse(content=content)


@app.post("/login")                                     # Gets username and password and checks if
async def get_credentials(login_item: LoginItem):       # credentials are in database. If they are
    username = login_item.username                      # then a token is generated and put into 
    password = login_item.password                      # database with expiration date. If they are
                                                        # not, then "Not Authorized" string is
    if username == None or password == None:            # returned.
        response = RedirectResponse(url="/login")       #-------------------------------------------
        return response

    else:
        try:
            cur = conn.cursor()

            cur.execute(
                "SELECT username, password FROM admins WHERE username=%s and password=%s;",     # Check DB
                (username, password),                                                           #---------
            )
            response_item = cur.fetchone()
            print(response_item)
            if response_item != None:
                print("AUTHENTICATED!")
                username = response_item[0]
                password = response_item[1]
                token = secrets.token_hex(32)                               # Generate token
                time_now = datetime.datetime.utcnow()                       #---------------
                time_change = datetime.timedelta(hours=2)
                time_cookie = time_now + time_change
                time_expiration = int(
                    datetime.datetime.strftime(time_cookie, "%y%m%d%H%M")
                )

                print(time_now)

                cur.execute(                                                # Set access token and
                                                                            # expiration
                                                                            #---------------------
                    "UPDATE admins \
                        SET token=%s, expiration=%s \
                            WHERE username=%s and password=%s;",
                    (token, time_expiration, username, password),
                )
                conn.commit()

                response = RedirectResponse(url="/home", status_code=303)
                response.set_cookie(key="TMP_ACCESS", value=token, expires=7200)    # Set access cookie
                return response                                                     #------------------
            else:
                print("NOT AUTHENTICATED")
                return Response(content="Not Authorized")

        except Exception as e:
            print(f"Problem fetching credentials --> {e}")


@app.get("/home")                                                   # Return page for updating user
async def home(request: Request):                                   # access credentials
    cookie = request.cookies.get("TMP_ACCESS")                      #------------------------------
    cur = conn.cursor()

    cur.execute("SELECT * FROM admins WHERE token=%s", (cookie,))   # Check for valid auth cookie
    admins_response_item = cur.fetchone()                           # and return to login if not
    if admins_response_item == None:                                #----------------------------
        return RedirectResponse(url="/login")
    else:
        cur.execute(
                    "CREATE TABLE IF NOT EXISTS auth(username text, pubkey blob)"
                )
        conn.commit()

        cur.execute("SELECT username, pubkey FROM auth")            # Get all usernames and pubkeys
        response_item = cur.fetchall()                              #------------------------------
        html_str = ""
        html_str += "<tr>\
                        <th style='width: 25%;'>Username</th>\
                        <th style='width: 75%;'>Public Key</th>\
                    </tr>"

        for obj in response_item:                                   # Generate HTML to display each
            username = obj[0]                                       # username and pubkey
            pubkey = str(base64.b64encode(obj[1]).decode("utf-8"))  #------------------------------
            html_str += f"<tr>\
                            <td style='position: relative; width: 25%;'>{username}<br><button \
                                class='btn btn-primary' id='{username}' style='position: absolute; \
                                bottom:0; margin-bottom: 8px;'>Delete</button></th>\
                            <td style='width: 75%;'>{pubkey}</th>\
                        </tr>"
            
        style="<style>a {color: rgb(36, 118, 224) !important;}\
                              table td {word-break: break-all;}\
                      a:hover{cursor: pointer;}\
                        </style>"
        

        return templates.TemplateResponse("home.html",              # Return template with credentials
                                          {"request": request,      #---------------------------------
                                           "html_str":html_str,
                                           "style": style})


@app.post("/auth")                                          # Used by lock's Bluetooth server to
async def read_root(request: Request):                      # authenticate a user
    json_bytes = await request.body()                       #-----------------------------------
    json_str = json_bytes.decode()
    print(json_str)
    json_obj = json.loads(json_str)

    print(json_obj['username'])

    username = json_obj["username"]                         # Get username and door number
    door_num = json_obj["door"]                             #-----------------------------

    cur = conn.cursor()


    cur.execute(
        "SELECT doors FROM auth WHERE username = %s",
        (username,),
    )

    response_item: str = cur.fetchone()


    if response_item == None:
        return Response(content="Not Authorized")
    else:
        door_list = response_item[0].split(',')
        if door_num not in door_list:                           # If user does not have access to
            return Response(content="Not Authorized")           # door number, then don't allow
        else:                                                   #--------------------------------
            cur.execute(
                                                                        # Get pubkey if username exists
                        "SELECT pubkey FROM auth WHERE username = %s",  # and user has access to door
                        (username,),                                    #------------------------------
                        )
            key = cur.fetchone()

            encoded_key = base64.b64encode(key[0])

            return Response(content=encoded_key)                # Return base64 encoded pubkey
                                                                #-----------------------------

@app.post("/create-credentials/{username}")                     # Creates credentials for a new user
async def upload(username: str,  request: Request):             #-----------------------------------

    cur = conn.cursor()

    cookie = request.cookies.get("TMP_ACCESS")                      # Verifies auth cookie
    cur.execute("SELECT * FROM admins WHERE token=%s", (cookie,))   #---------------------
    admins_response_item = cur.fetchone()
    if admins_response_item == None:
        return RedirectResponse(url="/login")
    else:

        if request.headers["content-type"] == "application/pkcs8":
            data: bytes = await request.body()
        if request.headers["content-type"] == "text/plain":
            data: bytes = await request.body()
            data = data.decode()

        if data != None and username != None:
            try:

                if request.headers["content-type"] == "application/pkcs8":  # Adds username and pubkey to
                    # remove previous credentials                           # to DB
                    cur.execute("DELETE FROM auth WHERE username = %s",     #----------------------------
                                (username,))                                
                    conn.commit()

                    # insert new credentials
                    cur.execute("INSERT INTO auth VALUES (%s, %s, %s)", 
                                (username, data, None))
                    conn.commit()

                if request.headers["content-type"] == "text/plain":               # Adds allowed doors to
                    cur.execute("UPDATE auth SET doors=%s WHERE username=%s",     # DB
                                (data, username))                                 #----------------------
                    conn.commit()                                                               

                return Response(content="file uploaded :)")
            except Exception:
                return {"message": "There was an error uploading the file"}

        else:
           return Response(content="nothing to upload!")
        
@app.post("/delete-credentials")                            # Deletes user credentials from DB
async def remove_credentials(request: Request):             #---------------------------------
    cur = conn.cursor()

    cookie = request.cookies.get("TMP_ACCESS")                      # Verifies auth cookie
    cur.execute("SELECT * FROM admins WHERE token=%s", (cookie,))   #---------------------
    admins_response_item = cur.fetchone()
    if admins_response_item == None:
        return RedirectResponse(url="/login")
    else:
        data = await request.json()
        username = data["username"]
        cur.execute("DELETE FROM auth WHERE username=%s", (username,))
        conn.commit()


if __name__ == "__main__":

    uvicorn.run("auth_server:app",                      # Start web server
                port=50002,                             #-----------------
                host="0.0.0.0",
                ssl_keyfile='/certs/privkey1.pem',      # Certificates used for https
                ssl_certfile='/certs/cert1.pem',        #----------------------------
                ssl_ca_certs='/certs/fullchain1.pem')
                                                        # NOTE: Certbot is great for
                                                        # generating these certificates
                                                        #------------------------------
