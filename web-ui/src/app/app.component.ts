
import { distinctUntilChanged } from 'rxjs/operators';
import { Component, OnInit } from '@angular/core';

import { Router, NavigationEnd } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';

import { DEFAULT_LANG, LanguageService } from './services/language.service';

import { environment } from '../environments/environment';

class Tab {
  label: string;
  path?: string;
  mainTab: boolean;
  hasChildren: boolean;
  children?: Array<any>;
  selector?: any;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  tabs: Tab[] = [
    {
      label: 'Dashboard',
      path: '/',
      mainTab: true,
      hasChildren: false,
      selector: ['']
    },
    {
      label: 'Nodes',
      path: '/nodes',
      mainTab: true,
      hasChildren: false,
      selector: ['nodes']
    },
    {
      label: 'Blocks',
      path: '/blocks',
      mainTab: true,
      hasChildren: false,
      selector: ['blocks']
    },
    {
      label: 'Transactions',
      path: '/transactions',
      mainTab: true,
      hasChildren: false,
      selector: ['transactions']
    },
    {
      label: 'Addresses',
      path: '/addresses',
      mainTab: true,
      hasChildren: false,
      selector: ['addresses']
    },
    {
      label: 'Tools',
      path: '/tools',
      mainTab: true,
      hasChildren: true,
      selector: ['trezor', 'calculator', 'governance', 'dex-monitor'],
      children: [{
        label: 'Trezor Wallet',
        path: '/trezor',
        mainTab: true,
        hasChildren: false,
        selector: '/tools'
      },{
        label: 'Calculator',
        path: '/calculator',
        mainTab: true,
        hasChildren: false,
        selector: '/tools'
      },{
        label: 'DEX Monitor',
        path: '/dex-monitor',
        mainTab: true,
        hasChildren: false,
        selector: '/tools'
      },{
        label: 'Governance',
        path: '/',
        mainTab: false,
        hasChildren: false,
        selector: '/tools'
      }]
    }
  ]

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
      'message.unavailable': '-',
      'message.serverUnavailable': 'The server is unavailable, please try again in a minute',
      'message.unknownErrors': 'Unknown error, please try again in a minute',
      'message.transactionNotFound': 'Transaction not found',
      'message.addressNotFound': 'Address not found',
      'message.blockNotFound': 'Block not found',
      'message.masternodeNotFound': 'Masternode not found',
      'message.loadingLatestBlocks': 'Loading latest blocks...',
      'message.loadingTransactions': 'Loading transactions...',
      'message.loadingRichestAddresses': 'Loading richest addresses...',
      'message.transactionsNotAvailable': 'The transactions are not available, please try again in some minutes',
      'message.totalSupply': 'The total number of XSN generated.',
      'message.circulatingSupply': 'The total number of XSN in circulation (excluding the burned coins.)',
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
      'label.searchField': 'Search by transaction ID, address, blockhash, block height, IP address',
      'label.transactionId': 'Transaction Id',
      'label.confirmations': 'Confirmations',
      'label.blockhash': 'Block Hash',
      'label.blocktime': 'Time',
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
      'label.showMore': 'Show More',
      'label.currentInflation': 'Current Inflation',
      'label.masternodeROI': 'Masternode ROI',
      'label.stakingROI': 'Staking ROI',
      'label.coinsInMasternodes': 'Coins in Masternodes',
      'label.tposNodes': 'TPoS Nodes',
      'label.coinsStaking': 'Coins Staking',
      'label.coinsTrustlesslyStaking': 'Coins Trustlessly Staking',
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
      'label.tposContractNotice': 'For now, this only works on Trezor T, once the contract gets created, send the amount you desire to stake to the contract Staking address, refresh the page after some minutes if you do not see such address',


      'label.address': 'Address',
      'label.addresses': 'Addresses',
      'label.ip': 'IP',
      'label.balance': 'Balance',
      'label.available': 'Available',
      'label.received': 'Received',
      'label.spent': 'Spent',
      'label.transactionCount': 'Transactions',
      'label.transactinoLabel': 'Transactions',

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
      'label.blockRewards': 'Block rewards',
      'label.blockRewardsEstimation': 'Block rewards estimation',
      'label.dexHostEstimation': 'Dex Host Estimation',
      'label.estRewardTime': 'Avg. Reward Frequency',
      'label.masternodesTVL': 'Masternodes TVL',
      'label.stakingTVL': 'Staking TVL',
      'label.totalCoinsMN+StakingTVL': 'Total coins MN+Staking TVL',
      'label.firstRewardWait': 'First Reward ETA',
      
      'label.blockHash': 'Block Hash',
      'label.txHash': 'TX Hash',
      'label.type': 'Type',
      'label.result': 'Result',
      'label.time': 'Time',

      'label.tposContract': 'Contract',
      'label.owner': 'Owner',
      'label.merchant': 'Merchant',
      'label.total': 'Total',
      'label.summary': 'Summary',
      'label.block': 'Block',
      'label.transaction': 'Transaction',
      'label.height': 'Height',
      'label.inflation': 'Inflation',
      'label.price': 'Price',
      'label.extractedBy': 'Extracted by',
      'label.latestBlocks': 'Latest 10 blocks',
      'label.totalSupply': 'Total Supply',
      'label.circulatingSupply': 'Circulating Supply',
      'label.blocks': 'Blocks',
      'label.richestAddresses': 'Richest addresses',
      'label.masternodes': 'Masternodes',
      'label.percentOfCoins': 'Percent of coins',
      'label.percentOfCircSupply': '% of circ. supply',
      'label.addressLabel': 'Label',
      'label.protocol': 'Protocol',
      'label.status': 'Status',
      'label.lastSeen': 'Last seen',
      'label.payee': 'Payee Address',
      'label.active': 'Active Since',
      'label.details': 'Details',
      'label.raw': 'Raw',
      'label.date': 'Date',
      'label.more': 'More',
      'label.enabled': 'Enabled',
      'label.distributedAcross': 'Distributed Across',
      'label.protocols': 'Protocols',
      'label.totalCoinsMNStaking': 'Total Coins MN+Staking',
      'label.currentBlockCount': 'Current Block Count',

      'label.isStakingCoinsTrustlessly': 'Is this address staking coins trustlessly?',
      'label.contractState': 'Contract state',
      'label.contractsFound': 'Contracts found',
      'label.contractTxid': 'Contract txid',

      'label.yes': 'Yes',
      'label.yesOwnerOfTheStakedCoins': 'Yes, it\'s the owner of the staked coins',
      'label.yesMerchantOfTheStakedCoins': 'Yes, it\'s the merchant of the staked coins',
      'label.no': 'No',
      'label.tposDetails': 'TPoS Details',

      'label.calculator': 'Calculator',
      'label.xsnAmountHold': 'Amount of XSN you hold',
      'label.optimalSetup': 'Optimal setup',
      'label.masternodeAnd': 'Masternode(s) and',
      'label.xsnStaking': 'XSN staking',
      'label.masternodeStakingRemaining': 'Masternode(s) + staking',
      'label.moreProfitable': 'is 33% more profitable',
      'label.stakingAllCoins': 'Staking',
      'label.daily': 'Daily',
      'label.monthly': 'Monthly',
      'label.yearly': 'Yearly',
      'label.days': 'days',
      'label.waitingTimePerReward': 'Waiting time per reward (est.)',
      'label.requiredForMasternode': 'Required for Masternode',
      'label.rewardPerBlock': 'Reward per Block',
      'label.avgRewardWaitTime': 'Avg. reward wait time',

      'label.xsnPrice': 'XSN Price',
      'label.totalMasternodes': 'Total Masternodes',
      'label.yourMasternodes': 'Your Masternodes',
      'label.orderbookRewards': 'Orderbook Rewards',
      'label.dailyTradingVolume': 'Daily Trading Volume',
      'label.mnsHostingDEX': 'MNs Hosting DEX',
      'label.enableDexHosting': 'Enable DEX hosting on your Masternode(s)?:',
      'label.calculatedBlockRewards': 'Calculated Block Rewards',
      'label.calculatedOrderbookHostingRewards': 'Calculated DEX Hosting Rewards',
      'label.masternodeHostingCost': 'Masternode Hosting Cost',
      'label.totalRewardsEST': 'Total rewards (est)',
      'label.daysUntilFreeMasternodeEarned': 'Days Until Free Masternode Earned',
      'label.monthsUntilFreeMasternodeEarned': 'Months Until Free Masternode Earned',
      'label.yearsUntilFreeMasternodeEarned': 'Years Until Free Masternode Earned',
      'label.roi': 'ROI',

      'help.xsnPrice': 'Estimated XSN Price.',
      'help.totalMasternodes': 'The estimated total amount of Masternodes within the Stakenet network.',
      'help.yourMasternodes': 'Number of Masternodes you own.',
      'help.enableDexHosting': 'Host the DEX on your Masternodes to earn the DEX fees.',
      'help.dailyTradingVolume': 'The estimated DEX daily trading volume.',
      'help.mnsHostingDEX': 'The estimated amount of Masternodes hosting the DEX.'
    };
  }
}
