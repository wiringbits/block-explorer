import { TestBed, async } from '@angular/core/testing';
import { AppComponent } from './app.component';

import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

import { DEFAULT_LANG, LanguageService } from './services/language.service';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('AppComponent', () => {

  const languageServiceSpy: jasmine.SpyObj<LanguageService> = jasmine.createSpyObj('LanguageService', ['getLang']);

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        AppComponent
      ],
      imports: [
        TranslateModule.forRoot(),
        RouterTestingModule
      ],
      providers: [
        { provide: LanguageService, useValue: languageServiceSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));
  it('should create the app', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));
});
