import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockDetailsComponent } from './block-details.component';
import { ExplorerCurrencyPipe } from '../../pipes/explorer-currency.pipe';
import { ExplorerDatetimePipe } from '../../pipes/explorer-datetime.pipe';

import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';

import { BlocksService } from './../../services/blocks.service';
import { ErrorService } from '../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('BlockDetailsComponent', () => {
  let component: BlockDetailsComponent;
  let fixture: ComponentFixture<BlockDetailsComponent>;

  const blocksServiceSpy: jasmine.SpyObj<BlocksService> = jasmine.createSpyObj('BlocksService', ['get', 'getTransactionsV2']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    blocksServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        BlockDetailsComponent,
        ExplorerCurrencyPipe,
        ExplorerDatetimePipe
      ],
      imports: [
        RouterTestingModule,
        TranslateModule.forRoot()
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
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
