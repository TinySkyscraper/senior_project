from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP
from Crypto.Signature import pss
from Crypto.Hash import SHA256
import sqlite3
import requests

string = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100b10cbc908e1e554ec9107b0ce2b0e3b48517506028bce8126a35db97fdbb09e56124da9026e88fbecd91586cbc624a93f4ae96d0d544e239666b0b8b3bf022317c0e4cdcb3d1c64a730b9cc82088faa2e748604247f104a09bacbc299114a8f7badba903fea5a5d3bbe81ea99037f07f6942da2c9a5fbf89c11166876768ff22761419aa32b20a1764976398da3858574a67e5a6a82676632b7333ee70df68b5e0898e24a43bff8174de263486be74c24d92c40eeed884751abb4c66ae470b52100c4bac05fda48687dc8c01ce45c2e9468bbf54840c20d59b5de3036c4d9e5e1008b3e9a3444c2bc9448e161b15e2dcbbc12f8c85900059d1e93d664e062e1b0203010001"
pubkey = bytes.fromhex(string)


def encrypt(pubkey, challenge_message):
    publickey = RSA.import_key(pubkey)
    sign_verifier = pss.new(publickey)
    hasher = SHA256.new()
    hasher.update(challenge_message)
    sign_verifier.verify()


def test_database(username, key):
    con = sqlite3.connect("localhost/access_control.db")
    cur = con.cursor()
    cur.execute("INSERT INTO auth VALUES (?, ?)", (username, key))
    con.commit()
    con.close()


if __name__ == "__main__":
    response = requests.get("http://google.com/")
    print(response.content)
