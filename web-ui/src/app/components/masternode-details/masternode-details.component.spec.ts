import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MasternodeDetailsComponent } from './masternode-details.component';
import { ExplorerDatetimePipe } from '../../pipes/explorer-datetime.pipe';

import { MomentModule } from 'ngx-moment';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

import { NavigatorService } from '../../services/navigator.service';
import { MasternodesService } from '../../services/masternodes.service';
import { ErrorService } from '../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('MasternodeDetailsComponent', () => {
  let component: MasternodeDetailsComponent;
  let fixture: ComponentFixture<MasternodeDetailsComponent>;

  const navigatorServiceSpy: jasmine.SpyObj<NavigatorService> = jasmine.createSpyObj('NavigatorService', ['']);
  const masternodesServiceSpy: jasmine.SpyObj<MasternodesService> = jasmine.createSpyObj('MasternodesService', ['getByIP']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    masternodesServiceSpy.getByIP.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        MasternodeDetailsComponent,
        ExplorerDatetimePipe
      ],
      imports: [
        MomentModule,
        TranslateModule.forRoot(),
        RouterTestingModule
      ],
      providers: [
        { provide: NavigatorService, useValue: navigatorServiceSpy },
        { provide: MasternodesService, useValue: masternodesServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
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
