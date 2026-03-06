import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SubseccionesPetrolera } from './subsecciones-petrolera';

describe('SubseccionesPetrolera', () => {
  let component: SubseccionesPetrolera;
  let fixture: ComponentFixture<SubseccionesPetrolera>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubseccionesPetrolera]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SubseccionesPetrolera);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
