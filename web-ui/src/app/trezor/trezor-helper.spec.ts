import {
  getAddressTypeByAddress,
  TrezorAddress,
  getAddressTypeByPrefix,
  convertToSatoshis,
  toTrezorReferenceTransaction,
  selectUtxos,
  toTrezorInput
} from '../trezor/trezor-helper';
import { UTXO } from '../models/utxo';
import { TposContract } from '../models/tpos-contract';

describe('getAddressTypeByAddress', function () {
  it('correct type by address', function () {
    expect(getAddressTypeByAddress('XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d')).toBe(TrezorAddress.LEGACY);
    expect(getAddressTypeByAddress('7iAriornLWgTxwnRg23wingNYbriX8E1Dx')).toBe(TrezorAddress.P2SHSEGWIT);
    expect(getAddressTypeByAddress('XtykCTAHXCfVP9tGwvR2jK5tjx1pM9uYmi')).toBe(TrezorAddress.LEGACY);
    expect(getAddressTypeByAddress('xc1qezj2rjer67cg4cmxtrsv44svt0xctcywclzk9m')).toBe(TrezorAddress.SEGWIT);
    expect(getAddressTypeByAddress('xc1quqcensgkx5cmm0c52evefpknd6pghuh606e2ty')).toBe(TrezorAddress.SEGWIT);
    expect(getAddressTypeByAddress('7ppJKMwbcJVAd2MWreLvAsZT8jeCQp9SR2')).toBe(TrezorAddress.P2SHSEGWIT);
    expect(getAddressTypeByAddress('7f1DPJXFw5qNs83Duq3tb68VQbGNqx55HV')).toBe(TrezorAddress.P2SHSEGWIT);
    expect(getAddressTypeByAddress('Xy9dabFLB6152gpLMeTn9TEQvrpptmMsjnT')).toBe(TrezorAddress.LEGACY);
    expect(() => { getAddressTypeByAddress(''); })
      .toThrow(new Error('Unknown address type'));
    expect(() => { getAddressTypeByAddress('_f1DPJXFw5qNs83Duq3tb68VQbGNqx55HV'); })
      .toThrow(new Error('Unknown address type'));
  });
});

describe('getAddressTypeByPrefix', function () {
  it('correct type by prefix', function () {
    expect(getAddressTypeByPrefix(44)).toBe(TrezorAddress.LEGACY);
    expect(getAddressTypeByPrefix(49)).toBe(TrezorAddress.P2SHSEGWIT);
    expect(getAddressTypeByPrefix(84)).toBe(TrezorAddress.SEGWIT);
    expect(() => { getAddressTypeByPrefix(0); })
      .toThrow(new Error('Unknown address type'));
  });
});

describe('convertToSatoshis', function () {
  it('correct value', function () {
    expect(convertToSatoshis(990)).toBe(99000000000);
    expect(convertToSatoshis(0.565)).toBe(56500000);
    expect(convertToSatoshis(0.000001)).toBe(100);
    expect(convertToSatoshis(0.00000001)).toBe(1);
    expect(convertToSatoshis(0.0000001)).toBe(10);
    expect(convertToSatoshis(77790.0000001)).toBe(7779000000010);
    expect(convertToSatoshis(9128383.0000001)).toBe(912838300000010);
    expect(convertToSatoshis(1)).toBe(100000000);
    expect(convertToSatoshis(99999.999999)).toBe(9999999999900);
    expect(convertToSatoshis(0)).toBe(0);
    expect(convertToSatoshis(0.000000001)).toBe(0);
    expect(() => { convertToSatoshis(-12); })
      .toThrow(new Error('Invalid negative amount'));
  });
});

describe('toTrezorReferenceTransaction', function () {
  const case1 = toTrezorReferenceTransaction({
    vin: [
      {
        txid: '358d8c9c2a8843cce58a7c35158f32213e1725d8c08e35a5b56ff319affe9ac9',
        vout: '0',
        scriptSig: {
          hex: '16001401d9fad5dd794980e90a0284d113a2b7c76cafef'
        },
        sequence: 4294967293
      }
    ],
    vout: [
      {
        value: 1.1,
        scriptPubKey: {
          hex: '76a914ef389b41f17223741592c9880682240f2774caa588ac'
        }
      }
    ],
    locktime: '478214',
    version: 2,
    txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae'
  });
  it('correct format', function () {
    expect(case1.lock_time).toEqual(jasmine.any(Number));
    expect(case1.version).toEqual(jasmine.any(Number));
    expect(case1.inputs).toEqual(jasmine.any(Array));
    expect(case1.bin_outputs).toEqual(jasmine.any(Array));
    expect(case1.hash).toEqual(jasmine.any(String));
    case1.inputs.forEach(item => {
      expect(item.prev_hash).toEqual(jasmine.any(String));
      expect(item.prev_index).toEqual(jasmine.any(String));
      expect(item.script_sig).toEqual(jasmine.any(String));
      expect(item.sequence).toEqual(jasmine.any(Number));
    });
    case1.bin_outputs.forEach(item => {
      expect(item.amount).toEqual(jasmine.any(Number));
      expect(item.script_pubkey).toEqual(jasmine.any(String));
    });
  });
  it('correct values', function () {
    expect(case1.lock_time).toBe(478214);
    expect(case1.version).toBe(2);
    expect(case1.hash).toBe('483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae');
    expect(case1.inputs[0].prev_hash).toBe('358d8c9c2a8843cce58a7c35158f32213e1725d8c08e35a5b56ff319affe9ac9');
    expect(case1.inputs[0].prev_index).toBe('0');
    expect(case1.inputs[0].script_sig).toBe('16001401d9fad5dd794980e90a0284d113a2b7c76cafef');
    expect(case1.inputs[0].sequence).toBe(4294967293);
    expect(case1.bin_outputs[0].amount).toBe(110000000);
    expect(case1.bin_outputs[0].script_pubkey).toBe('76a914ef389b41f17223741592c9880682240f2774caa588ac');
  });
});

describe('selectUtxos', function () {

  it('returns the correct values', function () {
    const utxos: UTXO[] = [
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d',
        satoshis: 90900111,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae',
        outputIndex: 1
      }
    ];
    const result = selectUtxos(utxos, 90900111);

    expect(result.satoshis).toBe(90900111);
    expect(result.utxos.length).toBe(1);
    expect(result.utxos[0].address).toBe('XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d');
    expect(result.utxos[0].satoshis).toBe(90900111);
    expect(result.utxos[0].script).toBe('76a914ef389b41f17223741592c9880682240f2774caa588ac');
    expect(result.utxos[0].txid).toBe('483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae');
    expect(result.utxos[0].outputIndex).toBe(1);
  });

  it('returns satoshis = 0 with no utxos when the target amount can not be covered', function () {
    const utxos: UTXO[] = [
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d',
        satoshis: 90900111,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae',
        outputIndex: 0
      }
    ];
    const result = selectUtxos(utxos, 90900112);

    // not enough utxo for current amount
    expect(result.satoshis).toBe(0);
    expect(result.utxos.length).toBe(0);
  });

  it('returns satoshis = 0 with no utxos when the target amount can not be covered because we need tpos contracts', function () {
    const tposTxid = '583bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae';
    const tposIndex = 2;
    const utxos: UTXO[] = [
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7e',
        satoshis: 1,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: tposTxid,
        outputIndex: tposIndex
      },
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d',
        satoshis: 10,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae',
        outputIndex: 0
      }
    ];

    const tpostContracts = [new TposContract(tposTxid, tposIndex, 'owner', 'merchant', 20, 1000000, 'ACTIVE')];
    const result = selectUtxos(utxos, 11, tpostContracts);

    // not enough utxo for current amount
    expect(result.satoshis).toBe(0);
    expect(result.utxos.length).toBe(0);
  });

  it('avoid using tpos contract collateral', function () {
    const tposTxid = '583bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae';
    const tposIndex = 2;
    const utxos: UTXO[] = [
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7e',
        satoshis: 1,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: tposTxid,
        outputIndex: tposIndex
      },
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d',
        satoshis: 10,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae',
        outputIndex: 0
      }
    ];

    const tpostContracts = [new TposContract(tposTxid, tposIndex, 'owner', 'merchant', 20, 1000000, 'ACTIVE')];
    const result = selectUtxos(utxos, 1, tpostContracts);

    expect(result.satoshis).toBe(10);
    expect(result.utxos.length).toBe(1);
    expect(result.utxos[0].satoshis).toBe(10);
    expect(result.utxos[0].txid).toBe('483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae');
    expect(result.utxos[0].outputIndex).toBe(0);
  });

  it('the minimum utxos to cover the amount', function () {
    const utxos: UTXO[] = [
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d',
        satoshis: 400,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae',
        outputIndex: 0
      },
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d',
        satoshis: 100,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae',
        outputIndex: 0
      },
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d',
        satoshis: 550,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae',
        outputIndex: 0
      },
      {
        address: 'XxVj9BqtZNZTwEeE9MdWF1prgc8SKVpZ7d',
        satoshis: 3000,
        script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
        txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae',
        outputIndex: 0
      }
    ];
    const result = selectUtxos(utxos, 1000);

    expect(result.satoshis).toBe(1050);
    expect(result.utxos.length).toBe(3);
  });
});


describe('toTrezorInput', function () {
  const trezorAddresses1: TrezorAddress[] = [
    {
      address: 'Xy9dabFLB652gpLMeTn9TEQvrpptmMsjnT',
      path: [2147483692, 2147483847, 0, 0, 34],
      serializedPath: `m/44'/199'/0'/0/34`
    }
  ];
  const utxo1: UTXO = {
    address: 'Xy9dabFLB652gpLMeTn9TEQvrpptmMsjnT',
    satoshis: 90900111,
    script: '76a914ef389b41f17223741592c9880682240f2774caa588ac',
    txid: '483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae',
    outputIndex: 0
  };
  const case1 = toTrezorInput(trezorAddresses1, utxo1);
  it('correct value', function () {
    expect(case1.address_n).toEqual([2147483692, 2147483847, 0, 0, 34]);
    expect(case1.prev_hash).toBe('483bdb594fe347052cb56c60000fba593ef7dee8bf3f069e616fbdf8353e38ae');
    expect(case1.prev_index).toBe(0);
    expect(case1.amount).toBe('90900111');
    expect(case1.script_type).toBe('SPENDADDRESS');
  });
  const utxo2: UTXO = {
    address: '7f1DPJXFw5qNs83Duq3tb68VQbGNqx55HV',
    satoshis: 744409933,
    script: '16001401d9fad5dd794980e90a0284d113a2b7c76cafef',
    txid: '358d8c9c2a8843cce58a7c35158f32213e1725d8c08e35a5b56ff319affe9ac9',
    outputIndex: 1
  };
  it('correct value not valid', function () {
    expect(() => { toTrezorInput(trezorAddresses1, utxo2); })
      .toThrow(new Error('Address not found'));
  });
  const trezorAddresses2: TrezorAddress[] = [
    {
      address: '7f1DPJXFw5qNs83Duq3tb68VQbGNqx55HV',
      path: [2147483692, 2147483847, 0, 0, 3],
      serializedPath: `m/44'/199'/0'/0/3`
    }
  ];
  const case2 = toTrezorInput(trezorAddresses2, utxo2);
  it('correct value', function () {
    expect(case2.address_n).toEqual([2147483692, 2147483847, 0, 0, 3]);
    expect(case2.prev_hash).toBe('358d8c9c2a8843cce58a7c35158f32213e1725d8c08e35a5b56ff319affe9ac9');
    expect(case2.prev_index).toBe(1);
    expect(case2.amount).toBe('744409933');
    expect(case2.script_type).toBe('SPENDP2SHWITNESS');
  });
  const trezorAddresses3: TrezorAddress[] = [
    {
      address: 'xc1quqcensgkx5cmm0c52evefpknd6pghuh606e2ty',
      path: [2147483692, 2147483847, 0, 1, 12],
      serializedPath: `m/44'/199'/0'/1/12`
    }
  ];
  const utxo3: UTXO = {
    address: 'xc1quqcensgkx5cmm0c52evefpknd6pghuh606e2ty',
    satoshis: 144401933,
    script: '16001401d9fad5dd794980e90a0284d113a2b7c76cafef',
    txid: '358d8c9c2a8843cce58a7c35158f32213e1725d8c08e35a5b56ff319affe9ac9',
    outputIndex: 2
  };
  const case3 = toTrezorInput(trezorAddresses3, utxo3);
  it('correct value', function () {
    expect(case3.address_n).toEqual([2147483692, 2147483847, 0, 1, 12]);
    expect(case3.prev_hash).toBe('358d8c9c2a8843cce58a7c35158f32213e1725d8c08e35a5b56ff319affe9ac9');
    expect(case3.prev_index).toBe(2);
    expect(case3.amount).toBe('144401933');
    expect(case3.script_type).toBe('SPENDWITNESS');
  });
});
