import { Component } from '@angular/core';

import { TranslateService } from '@ngx-translate/core';

import { DEFAULT_LANG, LanguageService } from './services/language.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  constructor(
    private translate: TranslateService,
    private languageService: LanguageService) {

    translate.setDefaultLang(DEFAULT_LANG);
    translate.use(languageService.getLang());

    // define langs
    translate.setTranslation('en', this.englishLang());
  }

  englishLang(): Object {
    return {
      // default messages from angular
      'required': 'A value is required',
      'pattern': 'Invalid format',
      'email': 'Invalid email',
      'minlength': 'More characters are required',
      'maxlength': 'Too many characters',
      'min': 'The value is too small',
      'max': 'The value is too big',

      // app specific values
      'PoW': 'Proof of Work',
      'PoS': 'Proof of Stake',
      'TPoS': 'Trustless Proof of Stake',

      // messages
      'message.unavailable': 'Unavailable',
      'message.serverUnavailable': 'The server unavailable, please try again in a minute',
      'message.unknownErrors': 'Unknown error, please try again in a minute',
      'message.transactionNotFound': 'Transaction not found',
      'message.addressNotFound': 'Address not found',
      'message.blockNotFound': 'Block not found',
      'message.loadingLatestBlocks': 'Loading latest blocks...',
      'message.loadingRichestAddresses': 'Loading richest addresses...',

      // error messages
      'error.nothingFound': 'That doesn\'t seem to be a valid address, nor valid block, neither a valid transaction',

      // actions
      'action.find': 'Find',

      // labels
      'label.coinName': 'XSN',
      'label.searchField': 'Transaction id or Blockhash or Address',
      'label.transactionId': 'Transaction Id',
      'label.confirmations': 'Confirmations',
      'label.blockhash': 'Block Hash',
      'label.blocktime': 'Block Time',
      'label.medianTime': 'Median Time',
      'label.noInput': 'No input',
      'label.coinbase': 'Coinbase',
      'label.output': 'Receivers',
      'label.from': 'From',
      'label.to': 'To',
      'label.value': 'Amount',
      'label.fee': 'Fee',

      'label.address': 'Address',
      'label.balance': 'Balance',
      'label.available': 'Available',
      'label.received': 'Received',
      'label.spent': 'Spent',
      'label.transactionCount': 'Transactions',

      'label.blockType': 'Block type',
      'label.next': 'Next',
      'label.previous': 'Previous',
      'label.merkleRoot': 'Merkle root',
      'label.size': 'Size',
      'label.version': 'Version',
      'label.nonce': 'Nonce',
      'label.bits': 'Bits',
      'label.chainwork': 'Chainwork',
      'label.difficulty': 'Difficulty',
      'label.transactions': 'Transactions',
      'label.rewards': 'Rewards',
      'label.coinstake': 'Coinstake',
      'label.masternode': 'Master node',
      'label.amount': 'Amount',
      'label.blockReward': 'Block reward',

      'label.tposContract': 'Contract',
      'label.owner': 'Owner',
      'label.merchant': 'Merchant',
      'label.total': 'Total',
      'label.summary': 'Summary',
      'label.block': 'Block',
      'label.transaction': 'Transaction',
      'label.height': 'Block height',
      'label.extractedBy': 'Extracted by',
      'label.latestBlocks': 'Latest 10 blocks',
      'label.totalSupply': 'Total supply',
      'label.circulatingSupply': 'Circulating supply',
      'label.blocks': 'Blocks',
      'label.richestAddresses': 'Richest addresses',
      'label.masternodes': 'Masternodes'
    };
  }
}
