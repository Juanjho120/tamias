import { NgClass } from '@angular/common';
import { Component, Input } from '@angular/core';

export type TamiRobotSize = 'sm' | 'md' | 'lg';
export type TamiRobotMode = 'idle' | 'hover' | 'speaking';

@Component({
  selector: 'app-tami-robot',
  standalone: true,
  imports: [NgClass],
  template: `
    <span
      class="tami-robot"
      [ngClass]="robotClasses"
      aria-hidden="true"
    >
      <span class="tami-robot__antenna"></span>
      <span class="tami-robot__head">
        <span class="tami-robot__eye tami-robot__eye--left"></span>
        <span class="tami-robot__eye tami-robot__eye--right"></span>
        <span class="tami-robot__mouth"></span>
      </span>
    </span>
  `,
  styles: [
    `
      .tami-robot {
        --tami-robot-size: 2rem;
        --tami-robot-primary: #0d6efd;
        --tami-robot-accent: #6f42c1;
        --tami-robot-face: #eef6ff;
        --tami-robot-line: rgba(13, 110, 253, 0.42);
        --tami-robot-eye: #0d6efd;
        --tami-robot-mouth: #6f42c1;

        position: relative;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: var(--tami-robot-size);
        height: var(--tami-robot-size);
        min-width: var(--tami-robot-size);
        vertical-align: middle;
      }

      .tami-robot--sm {
        --tami-robot-size: 1.55rem;
      }

      .tami-robot--lg {
        --tami-robot-size: 2.55rem;
      }

      .tami-robot__head {
        position: relative;
        display: inline-block;
        width: calc(var(--tami-robot-size) * 0.86);
        height: calc(var(--tami-robot-size) * 0.72);
        border: 2px solid var(--tami-robot-line);
        border-radius: 42% 42% 36% 36%;
        background:
          radial-gradient(circle at 25% 20%, rgba(255, 255, 255, 0.95) 0 12%, transparent 13%),
          linear-gradient(135deg, var(--tami-robot-face), #ffffff 72%);
        box-shadow: 0 0.35rem 0.8rem rgba(13, 110, 253, 0.16);
      }

      .tami-robot__antenna {
        position: absolute;
        top: calc(var(--tami-robot-size) * -0.04);
        left: 50%;
        width: 2px;
        height: calc(var(--tami-robot-size) * 0.22);
        background: var(--tami-robot-line);
        transform: translateX(-50%);
      }

      .tami-robot__antenna::before {
        content: '';
        position: absolute;
        top: -0.22rem;
        left: 50%;
        width: 0.36rem;
        height: 0.36rem;
        border-radius: 999px;
        background: linear-gradient(135deg, var(--tami-robot-primary), var(--tami-robot-accent));
        transform: translateX(-50%);
        box-shadow: 0 0 0.45rem rgba(13, 110, 253, 0.35);
      }

      .tami-robot__eye {
        position: absolute;
        top: 34%;
        width: calc(var(--tami-robot-size) * 0.12);
        height: calc(var(--tami-robot-size) * 0.12);
        border-radius: 999px;
        background: var(--tami-robot-eye);
        box-shadow: 0 0 0.3rem rgba(13, 110, 253, 0.35);
      }

      .tami-robot__eye--left {
        left: 27%;
      }

      .tami-robot__eye--right {
        right: 27%;
      }

      .tami-robot__mouth {
        position: absolute;
        left: 50%;
        bottom: 22%;
        width: calc(var(--tami-robot-size) * 0.28);
        height: 2px;
        border-radius: 999px;
        background: var(--tami-robot-mouth);
        transform: translateX(-50%);
        transform-origin: center;
      }

      .tami-robot--hover:hover .tami-robot__head,
      .tami-robot--hover:focus-within .tami-robot__head {
        animation: tamiRobotHover 0.72s ease-in-out;
      }

      .tami-robot--hover:hover .tami-robot__antenna::before,
      .tami-robot--hover:focus-within .tami-robot__antenna::before {
        animation: tamiRobotGlow 0.72s ease-in-out;
      }

      .tami-robot--speaking .tami-robot__mouth {
        animation: tamiRobotTalk 0.22s ease-in-out infinite;
      }

      .tami-robot--speaking .tami-robot__eye {
        animation: tamiRobotBlink 1.35s ease-in-out infinite;
      }

      @keyframes tamiRobotHover {
        0%, 100% { transform: translateY(0) rotate(0deg); }
        30% { transform: translateY(-2px) rotate(-5deg); }
        65% { transform: translateY(1px) rotate(4deg); }
      }

      @keyframes tamiRobotGlow {
        0%, 100% { box-shadow: 0 0 0.45rem rgba(13, 110, 253, 0.35); }
        50% { box-shadow: 0 0 0.9rem rgba(111, 66, 193, 0.48); }
      }

      @keyframes tamiRobotTalk {
        0%, 100% { height: 2px; border-radius: 999px; }
        50% { height: calc(var(--tami-robot-size) * 0.1); border-radius: 45%; }
      }

      @keyframes tamiRobotBlink {
        0%, 92%, 100% { transform: scaleY(1); }
        95% { transform: scaleY(0.18); }
      }
    `
  ]
})
export class TamiRobotComponent {
  @Input() size: TamiRobotSize = 'md';
  @Input() mode: TamiRobotMode = 'idle';

  get robotClasses(): string[] {
    return [`tami-robot--${this.size}`, `tami-robot--${this.mode}`];
  }
}
