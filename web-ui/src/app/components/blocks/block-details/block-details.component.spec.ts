import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockDetailsComponent } from './block-details.component';
import { ExplorerCurrencyPipe } from '../../../pipes/explorer-currency.pipe';
import { ExplorerDatetimePipe } from '../../../pipes/explorer-datetime.pipe';

import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { MomentModule } from 'ngx-moment';
import { BlocksService } from '../../../services/blocks.service';
import { ErrorService } from '../../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

const sampleBlockDetails = {
  'block': {
    'hash': '225f7e1388f4d4199033ff077d0ece9aaa04d82fda8cc3c5789fa18f2a84b597',
    'previousBlockhash': '83b8ebe36a88e6a2df01abcd97aae6c908be9088e94379f9cc4f533a171db43f',
    'nextBlockhash': 'dc4cfb8f386e5cedbbe002da267a19daeb2598ce500bd1bb04c1fa59cffbbf99',
    'merkleRoot': '29d826f65e46a9cdb09740a576cb066ba10e4acdb27977340a6a9b571042bf76',
    'transactions': [
      'b2b3029d694ce3ad1cbbc3f49f5d1c94b63a313ab4bc26a13a5094826b305ef8',
      'fc9d478982c89b1e309bb42bd1ce64c5a99f7a818d5e9e4920a6a70a31bd168e'
    ],
    'confirmations': 3,
    'size': 490,
    'height': 1277693,
    'version': 536870912,
    'time': 1597940753,
    'medianTime': 1597940526,
    'nonce': 0,
    'bits': '1c0143a1',
    'chainwork': '0000000000000000000000000000000000000000000000000c4daa33831c9b22',
    'difficulty': 202.5004526306896,
    'tposContract': '6170c1916b53f7c18353dc6271efa53bf17f43635de571976a9a7719fad76be0'
  },
  'rewards': {
    'owner': {
      'address': 'XvfB23af9aFfqZ2NFGqqwsUcitUkKH5RwM',
      'value': 8.73
    },
    'merchant': {
      'address': 'XczvAzAPnqAa2hQL3ip1rJHmiBoZ6E5RGM',
      'value': 0.27
    },
    'masternode': {
      'address': 'XwyktBrhrGct4BuJCt3GAPvV6jNyo4Stfw',
      'value': 9
    },
    'coinstake': null,
    'reward': null,
    'stakedAmount': 2008.82863013,
    'stakedDuration': 358545
  }
};

describe('BlockDetailsComponent', () => {
  let component: BlockDetailsComponent;
  let fixture: ComponentFixture<BlockDetailsComponent>;

  const blocksServiceSpy: jasmine.SpyObj<BlocksService> = jasmine.createSpyObj('BlocksService', ['getTransactionsV2', 'pipe', 'tape']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    blocksServiceSpy.getTransactionsV2.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        BlockDetailsComponent,
        ExplorerCurrencyPipe,
        ExplorerDatetimePipe
      ],
      imports: [
        RouterTestingModule,
        TranslateModule.forRoot(),
        MomentModule
      ],
      providers: [
        { provide: BlocksService, useValue: blocksServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BlockDetailsComponent);
    component = fixture.componentInstance;
    component.blockDetails = sampleBlockDetails;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
