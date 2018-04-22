import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MasternodesComponent } from './masternodes.component';

describe('MasternodesComponent', () => {
  let component: MasternodesComponent;
  let fixture: ComponentFixture<MasternodesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MasternodesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MasternodesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
