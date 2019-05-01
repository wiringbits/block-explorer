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

### GET /v2/addresses/:address/transactions

Retrieves the transactions for an address: `curl "https://xsnexplorer.io/api/xsn/v2/addresses/7jqffPwhzkVGbYFf525muhas6uVrhERwAm/transactions?limit=2&order=asc"`

Paginate by the last seen transaction: `curl "https://xsnexplorer.io/api/xsn/v2/addresses/7jqffPwhzkVGbYFf525muhas6uVrhERwAm/transactions?limit=2&order=asc&lastSeenTxid=55df1af4754f8890183ab733a6e1760b22eb18b72c1d416a15f8a2743eb0b0d0"`

Note: the ordering is optional.



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
Retrieves the transactions on the given block: `curl "https://xsnexplorer.io/api/xsn/v2/blocks/31791420aa1d096c84317a3ba40f448ee5fe5a1c6f8c21a02f026e5135b624b4/transactions?limit=2"`

Paginate by the last seen transaction: `curl "https://xsnexplorer.io/api/xsn/v2/blocks/31791420aa1d096c84317a3ba40f448ee5fe5a1c6f8c21a02f026e5135b624b4/transactions?limit=2&lastSeenTxid=48579eae0cbcb7e17feed2fd64d8106f8d21961bce141efb5139463ad25f4c61"`
