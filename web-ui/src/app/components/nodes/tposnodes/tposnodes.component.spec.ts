import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TposnodesComponent } from './tposnodes.component';

import { MomentModule } from 'ngx-moment';
import { NgxPaginationModule } from 'ngx-pagination';
import { TranslateModule } from '@ngx-translate/core';

import { TposnodesService } from '../../../services/tposnodes.service';
import { ErrorService } from '../../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('TposnodesComponent', () => {
  let component: TposnodesComponent;
  let fixture: ComponentFixture<TposnodesComponent>;

  const tposnodesServiceSpy: jasmine.SpyObj<TposnodesService> = jasmine.createSpyObj('TposnodesService', ['get']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    tposnodesServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [TposnodesComponent],
      imports: [
        MomentModule,
        NgxPaginationModule,
        TranslateModule.forRoot()
      ],
      providers: [
        { provide: TposnodesService, useValue: tposnodesServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TposnodesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
