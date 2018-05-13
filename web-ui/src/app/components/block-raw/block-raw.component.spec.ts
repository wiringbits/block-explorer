import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockRawComponent } from './block-raw.component';

describe('BlockRawComponent', () => {
  let component: BlockRawComponent;
  let fixture: ComponentFixture<BlockRawComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ BlockRawComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BlockRawComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
