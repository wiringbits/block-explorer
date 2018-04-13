import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RichestAddressesComponent } from './richest-addresses.component';

describe('RichestAddressesComponent', () => {
  let component: RichestAddressesComponent;
  let fixture: ComponentFixture<RichestAddressesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RichestAddressesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RichestAddressesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
