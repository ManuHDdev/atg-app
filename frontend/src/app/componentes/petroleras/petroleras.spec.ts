import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Petroleras } from './petroleras';

describe('Petroleras', () => {
  let component: Petroleras;
  let fixture: ComponentFixture<Petroleras>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Petroleras]
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
