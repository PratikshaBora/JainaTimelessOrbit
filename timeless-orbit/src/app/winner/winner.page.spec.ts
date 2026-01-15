import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WinnerPage } from './winner.page';

describe('WinnerPage', () => {
  let component: WinnerPage;
  let fixture: ComponentFixture<WinnerPage>;

  beforeEach(() => {
    fixture = TestBed.createComponent(WinnerPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
