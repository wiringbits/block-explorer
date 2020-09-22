
import { distinctUntilChanged } from 'rxjs/operators';
import { Component, OnInit } from '@angular/core';

import { Router, NavigationEnd } from '@angular/router';



import { TranslateService } from '@ngx-translate/core';

import { DEFAULT_LANG, LanguageService } from './services/language.service';

import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  constructor(
    private translate: TranslateService,
    private languageService: LanguageService,
    private router: Router) {

    translate.setDefaultLang(DEFAULT_LANG);
    translate.use(languageService.getLang());

    // define langs
    translate.setTranslation('en', this.englishLang());
  }

  ngOnInit() {
    // integrate google analytics via gtag - based on https://stackoverflow.com/a/47658214/3211175
    this.router.events.pipe(distinctUntilChanged((previous: any, current: any) => {
      // Subscribe to any `NavigationEnd` events where the url has changed
      if (current instanceof NavigationEnd) {
        return previous.url === current.url;
      }

      return true;
    })).subscribe((x: any) => {
      const dirtyUrl: string = x.url || '';
      const url = this.removeQueryParams(dirtyUrl);
      (<any>window).gtag('config', environment.gtag.id, { 'page_path': url });
    });
  }

  private removeQueryParams(url: string): string {
    const index = url.indexOf('?');
    if (index >= 0) {
      return url.substring(0, index);
    } else {
      return url;
    }
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
      'message.masternodeNotFound': 'Masternode not found',
      'message.loadingLatestBlocks': 'Loading latest blocks...',
      'message.loadingRichestAddresses': 'Loading richest addresses...',
      'message.transactionsNotAvailable': 'The transactions are not available, please try again in some minutes',
      'message.totalSupply': 'The total number of coins generated (excluding the burned coins)',
      'message.circulatingSupply': 'The total number of coins that are in circulation (excludes burned coins and the treasury)',
      'messages.invalidScriptType': 'Unknown script type',
      'messages.invalidAddressType': 'Unknown address type',
      'messages.invalidNegativeAmount': 'Invalid negative amount',
      'message.more': 'More',

      // error messages
      'error.nothingFound': 'That doesn\'t seem to be a valid address, nor valid block, neither a valid transaction or ip address',

      // actions
      'action.find': 'Find',
      'action.verifyAddress': 'Show full address',

      // labels
      'label.searchField': 'Transaction id, Blockhash, Block number, Address, IP address',
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
      'label.receiveXSN': 'Receive XSN',
      'label.sendXSN': 'Send XSN',
      'label.tpos': 'TPOS',
      'label.send': 'Send',
      'label.resetWallet': 'Reset Wallet',
      'label.highFee': 'High',
      'label.normalFee': 'Normal',
      'label.lowFee': 'Low',
      'label.satoshis': 'Satoshis',
      'label.merchantAddress': 'Merchant Address',
      'label.ownerAddress': 'Owner Address',
      'label.merchantCommission': 'Merchant Commission',
      'label.state': 'State',
      'label.commision': 'Commision',
      'label.add': 'Add',
      'label.enterMerchantAddress': 'Enter merchant address',
      'label.tposAddress': 'TPoS Address',
      'label.path': 'Path',
      'label.generatedTransaction': 'Generated transaction id',
      'label.createContract': 'Create TPoS contract',


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
      'label.masternode': 'Masternode',
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
      'label.masternodes': 'Masternodes',
      'label.percentOfCoins': 'Percent of coins',
      'label.addressLabel': 'Label',
      'label.protocol': 'Protocol',
      'label.status': 'Status',
      'label.lastSeen': 'Last seen',
      'label.payee': 'Payee',
      'label.active': 'Active',
      'label.details': 'Details',
      'label.raw': 'Raw',
      'label.date': 'Date',
      'label.more': 'More'
    };
  }
}
