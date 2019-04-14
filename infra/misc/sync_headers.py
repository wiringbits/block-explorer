#!/bin/python
#
# Traverses all headers for a single coin.

import requests
import sys

base_url = 'https://xsnexplorer.io/api'

def get_next(coin, last_seen_hash = ''):
    url = base_url + '/' + coin + '/blocks/headers?limit=1000'
    if last_seen_hash != '':
        url = url + '&lastSeenHash=' + last_seen_hash
    return requests.get(url).json()

def get_all(coin, trace = '', last_seen_hash = ''):
    print('getting from ' + trace)
    data = get_next(coin, last_seen_hash)['data']
    if len(data) == 0:
        print('done\n')
    else:
        get_all(coin, str(data[-1]['height']), data[-1]['hash'])

if __name__ == "__main__":
    sys.setrecursionlimit(999999999)
    get_all('ltc', '0')
