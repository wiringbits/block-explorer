import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MasternodesComponent } from './masternodes.component';

import { MomentModule } from 'ngx-moment';
import { NgxPaginationModule } from 'ngx-pagination';
import { TranslateModule } from '@ngx-translate/core';

import { MasternodesService } from '../../../services/masternodes.service';
import { ErrorService } from '../../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('MasternodesComponent', () => {
  let component: MasternodesComponent;
  let fixture: ComponentFixture<MasternodesComponent>;

  const masternodesServiceSpy: jasmine.SpyObj<MasternodesService> = jasmine.createSpyObj('MasternodesService', ['get']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    masternodesServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [MasternodesComponent],
      imports: [
        MomentModule,
        NgxPaginationModule,
        TranslateModule.forRoot()
      ],
      providers: [
        { provide: MasternodesService, useValue: masternodesServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
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
