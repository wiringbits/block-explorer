import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { NodeListComponent } from './node-list.component';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('NodeListComponent', () => {
    let component: NodeListComponent;
    let fixture: ComponentFixture<NodeListComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                NodeListComponent
            ],
            imports: [
                TranslateModule.forRoot()
            ],
            schemas: [NO_ERRORS_SCHEMA],
            providers: [
            ]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(NodeListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
