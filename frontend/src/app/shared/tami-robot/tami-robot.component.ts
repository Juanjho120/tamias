import { NgClass } from '@angular/common';
import { Component, Input } from '@angular/core';

export type TamiRobotSize = 'sm' | 'md' | 'lg';
export type TamiRobotMode = 'idle' | 'hover' | 'speaking';

@Component({
  selector: 'app-tami-robot',
  standalone: true,
  imports: [NgClass],
  template: `
    <span class="tami-robot" [ngClass]="robotClasses" aria-hidden="true" [style.--tami-mouth-animation-duration.ms]="mouthAnimationDurationMs">
      <span class="tami-robot__spark tami-robot__spark--left"></span>
      <span class="tami-robot__spark tami-robot__spark--right"></span>
      <span class="tami-robot__arm tami-robot__arm--left"></span>
      <span class="tami-robot__arm tami-robot__arm--right"></span>
      <span class="tami-robot__body"></span>
      <span class="tami-robot__badge"></span>
      <span class="tami-robot__neck"></span>
      <span class="tami-robot__head">
        <span class="tami-robot__antenna"></span>
        <span class="tami-robot__cap"></span>
        <span class="tami-robot__ear tami-robot__ear--left"></span>
        <span class="tami-robot__ear tami-robot__ear--right"></span>
        <span class="tami-robot__eye tami-robot__eye--left"></span>
        <span class="tami-robot__eye tami-robot__eye--right"></span>
        <span class="tami-robot__cheek tami-robot__cheek--left"></span>
        <span class="tami-robot__cheek tami-robot__cheek--right"></span>
        <span class="tami-robot__mouth"></span>
      </span>
    </span>
  `
})
export class TamiRobotComponent {
  @Input() size: TamiRobotSize = 'md';
  @Input() mode: TamiRobotMode = 'idle';
  @Input() mouthAnimationDurationMs = 170;

  get robotClasses(): string[] {
    return [`tami-robot--${this.size}`, `tami-robot--${this.mode}`];
  }
}
