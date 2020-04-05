import sys
from socket import *
cnt = 0
s = socket(AF_INET, SOCK_DGRAM)
with open(sys.argv[-1]) as f:
    for line in f.readlines():
        b = line.encode('ascii')
        print('sending: ' + str(b))
        s.sendto(line.encode('ascii'), ('localhost', 10666))
        cnt = cnt + 1

print("SENT " + str(cnt) + " events")
