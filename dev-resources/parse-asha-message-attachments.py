import base64
import sys
import xml.etree.ElementTree as ET

for document in ET.fromstring(sys.stdin.read()).findall('createDocument'):
    print(base64.standard_b64decode(document.find('contentStream').find('stream').text))
