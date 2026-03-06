import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SocioForm } from './socio-form';

describe('SocioForm', () => {
  let component: SocioForm;
  let fixture: ComponentFixture<SocioForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SocioForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SocioForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
