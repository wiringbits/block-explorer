import { ExplorerDatetimePipe } from './explorer-datetime.pipe';
import { pipeDef } from '../../../node_modules/@angular/core/src/view';

describe('ExplorerDatetimePipe', () => {

  beforeAll(() => {
    jasmine.clock().install();
  });

  afterAll(() => {
    jasmine.clock().uninstall();
  });

  it('create an instance', () => {
    const pipe = new ExplorerDatetimePipe('en');
    expect(pipe).toBeTruthy();
  });

  it('should return the parsed date when a Date object is sent', () => {
    const pipe = new ExplorerDatetimePipe('en');
    const date = new Date('1990-02-25');

    expect(pipe.transform(date)).toMatch('February 24, 1990, 6:00:00 PM');
  });

  it('should return the parsed date when a number is sent', () => {
    const pipe = new ExplorerDatetimePipe('en');
    const dateInMs = new Date('August 19, 1975 23:15:30').getTime();

    expect(pipe.transform(dateInMs)).toMatch('August 19, 1975, 11:15:30 PM')
  });

  it('should return the parsed date when a string is sent', () => {
    const pipe = new ExplorerDatetimePipe('en');
    const stringDate = 'August 19, 1975 23:15:30';

    expect(pipe.transform(stringDate)).toMatch('August 19, 1975, 11:15:30 PM')
  });
});
