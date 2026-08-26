import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { SocioDetail } from './socio-detail';

describe('SocioDetail', () => {
  let component: SocioDetail;
  let fixture: ComponentFixture<SocioDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SocioDetail],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
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
