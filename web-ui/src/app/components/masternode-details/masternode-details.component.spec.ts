import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MasternodeDetailsComponent } from './masternode-details.component';

describe('MasternodeDetailsComponent', () => {
  let component: MasternodeDetailsComponent;
  let fixture: ComponentFixture<MasternodeDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MasternodeDetailsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MasternodeDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
