import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Petroleras } from './petroleras';

describe('Petroleras', () => {
  let component: Petroleras;
  let fixture: ComponentFixture<Petroleras>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Petroleras],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Petroleras);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
