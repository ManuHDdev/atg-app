import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PetroleraForm } from './petrolera-form';

describe('PetroleraForm', () => {
  let component: PetroleraForm;
  let fixture: ComponentFixture<PetroleraForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PetroleraForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PetroleraForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
