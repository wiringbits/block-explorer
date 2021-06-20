import {truncate} from './utils';

describe('Utils testing', () => {
  it('should truncate the string', (() => {
    expect(truncate('1234567890abcde', 5, 5, '-')).toEqual('12345-abcde');
    expect(truncate('1234567890abcde', 9, 5)).toEqual('123456789 ... abcde');
  }));

  it('shouldn\'t truncate string with short length', (() => {
    expect(truncate('1234567890abcde', 10, 5, '-')).toEqual('1234567890abcde');
    expect(truncate('1234567890abcde', 11, 10)).toEqual('1234567890abcde');
  }));
});
