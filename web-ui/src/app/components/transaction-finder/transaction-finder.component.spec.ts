import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TransactionFinderComponent } from './transaction-finder.component';

describe('TransactionFinderComponent', () => {
  let component: TransactionFinderComponent;
  let fixture: ComponentFixture<TransactionFinderComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TransactionFinderComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TransactionFinderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
