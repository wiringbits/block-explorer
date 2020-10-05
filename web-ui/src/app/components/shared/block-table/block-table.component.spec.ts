import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockTableComponent } from './block-table.component';

import { MomentModule } from 'ngx-moment';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

import { BlocksService } from '../../../services/blocks.service';
import { ErrorService } from '../../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('BlockTableComponent', () => {
  let component: BlockTableComponent;
  let fixture: ComponentFixture<BlockTableComponent>;

  const blocksServiceSpy: jasmine.SpyObj<BlocksService> = jasmine.createSpyObj('BlocksService', ['getLatest']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    blocksServiceSpy.getLatest.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        BlockTableComponent
      ],
      imports: [
        TranslateModule.forRoot(),
        MomentModule,
        RouterTestingModule
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
    fixture = TestBed.createComponent(BlockTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
