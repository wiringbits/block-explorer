package com.xsn.explorer.models

/**
 * The package represents the models that are persisted in the explorer's database.
 *
 * One reason why we require different models than what is used on the RPC calls
 * is that there are fields which shouldn't be persisted, for example, the number
 * of confirmations for a block.
 */
package object persisted {}
