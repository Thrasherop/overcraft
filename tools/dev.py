"""Talks to the dev client's DevServer (gradlew runClient starts it on 127.0.0.1:25599; runClient2 on 25600: set
OC_DEV_PORT=25600 to talk to that one).

    py tools/dev.py state
    py tools/dev.py "key jump tap" "screenshot jump.png"      several commands in order
    py tools/dev.py --wait 2 "key forward down" "screenshot run.png" "key forward up"   --wait N: seconds between commands

Screenshots land in run/screenshots/.
"""
import json
import os
import socket
import sys
import time


def send(sock_file, sock, command):
    sock.sendall((command + "\n").encode("utf8"))
    return json.loads(sock_file.readline())


def main():
    args = sys.argv[1:]
    wait = 0.0
    if args[:1] == ["--wait"]:
        wait = float(args[1])
        args = args[2:]
    port = int(os.environ.get("OC_DEV_PORT", "25599"))
    with socket.create_connection(("127.0.0.1", port), timeout=40) as sock:
        f = sock.makefile("r", encoding="utf8")
        for i, command in enumerate(args):
            if command.startswith("sleep "):
                time.sleep(float(command.split()[1]))
                continue
            print(command, "->", json.dumps(send(f, sock, command)))
            if wait and i < len(args) - 1:
                time.sleep(wait)


if __name__ == "__main__":
    main()
