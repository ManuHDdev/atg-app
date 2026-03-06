import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SocioDetail } from './socio-detail';

describe('SocioDetail', () => {
  let component: SocioDetail;
  let fixture: ComponentFixture<SocioDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SocioDetail]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SocioDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
