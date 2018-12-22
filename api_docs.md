# Explorer API
Disclaimer: This documentation might have issues, the best way to get understand how is the API used is to use the developer tools from your favorite browser while browsing the explorer website.

All endpoints are accessible at `https://xsnexplorer.io/api/xsn/`, for accessing the `/health` endpoint, load `https://xsnexplorer.io/api/xsn/health`


## Statistics

## GET /stats

Retrieves statistics: `curl https://xsnexplorer.io/api/xsn/stats`


## Transactions

### GET /transactions/:txid

Retrieves a transaction by id: `curl https://xsnexplorer.io/api/xsn/transactions/efa97e3f55d3722df678986c7c275d63e8a2aa169b164c932c1f6dd8755356ad`

### GET /transactions/:txid/raw
Retrieves the raw transaction by id: `curl https://xsnexplorer.io/api/xsn/stats`



## Addresses

### GET /addresses/:address
Retrieves info about an address: `curl https://xsnexplorer.io/api/xsn/addresses/XvoBWMPo1NFstmCCTWZQ8w4BcmHXddNLxX`

### GET /addresses/:address/transactions

Retrieves the transactions for an address: `curl "https://xsnexplorer.io/api/xsn/addresses/XvoBWMPo1NFstmCCTWZQ8w4BcmHXddNLxX/transactions?offset=0&limit=10&orderBy=time:desc"`

Note: ordering and pagination are optional.



## Blocks

### GET /blocks

Retrieves the latest blocks: `curl https://xsnexplorer.io/api/xsn/blocks`

### GET /blocks/:query
Retrieves a block: `curl https://xsnexplorer.io/api/xsn/blocks/20`

Note: `:query` could be a blockhash or block height.

### GET /blocks/:query/raw
Retrieves a raw block: `curl https://xsnexplorer.io/api/xsn/blocks/20/raw`

Note: `:query` could be a blockhash or block height.

### GET /blocks/:blockhash/transactions
Retrieves the transactions on the given block: `curl "https://xsnexplorer.io/api/xsn/blocks/31791420aa1d096c84317a3ba40f448ee5fe5a1c6f8c21a02f026e5135b624b4/transactions?offset=0&limit=5&orderBy=time:desc"`

Note: ordering and pagination are optional.
