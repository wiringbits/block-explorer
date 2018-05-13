import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TransactionRawComponent } from './transaction-raw.component';

describe('TransactionRawComponent', () => {
  let component: TransactionRawComponent;
  let fixture: ComponentFixture<TransactionRawComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TransactionRawComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TransactionRawComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
