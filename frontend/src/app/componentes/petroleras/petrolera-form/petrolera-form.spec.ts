import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { PetroleraForm } from './petrolera-form';

describe('PetroleraForm', () => {
  let component: PetroleraForm;
  let fixture: ComponentFixture<PetroleraForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PetroleraForm],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
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
