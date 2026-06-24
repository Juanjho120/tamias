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
  `,
  styles: [
    `
      .tami-robot {
        --tami-robot-size: 2rem;
        --tami-robot-primary: #34a9a1;
        --tami-robot-accent: #ffd166;
        --tami-robot-face: #f8ffff;
        --tami-robot-line: rgba(36, 122, 118, 0.44);
        --tami-robot-eye: #247a76;
        --tami-robot-mouth: #e95f8a;
        --tami-robot-apron: #ffffff;
        --tami-robot-apron-trim: #d8f4f1;
        --tami-robot-shadow: rgba(25, 64, 61, 0.18);
        position: relative;
        display: inline-flex;
        align-items: flex-end;
        justify-content: center;
        width: calc(var(--tami-robot-size) * 1.18);
        height: calc(var(--tami-robot-size) * 1.36);
        min-width: calc(var(--tami-robot-size) * 1.18);
        vertical-align: middle;
        overflow: visible;
      }

      .tami-robot--sm { --tami-robot-size: 1.52rem; }
      .tami-robot--lg { --tami-robot-size: 2.7rem; }

      .tami-robot__body {
        position: absolute;
        left: 50%;
        bottom: calc(var(--tami-robot-size) * 0.02);
        z-index: 1;
        width: calc(var(--tami-robot-size) * 0.72);
        height: calc(var(--tami-robot-size) * 0.5);
        border: 2px solid var(--tami-robot-line);
        border-radius: 0.9rem 0.9rem 0.55rem 0.55rem;
        background: linear-gradient(180deg, #f7ffff 0%, #ddf4f2 100%);
        box-shadow: 0 0.22rem 0.6rem var(--tami-robot-shadow);
        transform: translateX(-50%);
        overflow: hidden;
      }

      .tami-robot__body::before {
        content: '';
        position: absolute;
        top: 17%;
        left: 15%;
        right: 15%;
        height: 17%;
        border-radius: 999px;
        background: rgba(52, 169, 161, 0.14);
      }

      .tami-robot__body::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: 7%;
        width: 58%;
        height: 62%;
        border: 1px solid rgba(36, 122, 118, 0.18);
        border-radius: 0.38rem 0.38rem 0.58rem 0.58rem;
        background:
          linear-gradient(90deg, transparent 48%, rgba(36, 122, 118, 0.13) 49%, rgba(36, 122, 118, 0.13) 51%, transparent 52%),
          linear-gradient(180deg, var(--tami-robot-apron), var(--tami-robot-apron-trim));
        transform: translateX(-50%);
      }

      .tami-robot__badge {
        position: absolute;
        top: calc(var(--tami-robot-size) * 0.9);
        right: calc(var(--tami-robot-size) * 0.26);
        z-index: 2;
        width: calc(var(--tami-robot-size) * 0.1);
        height: calc(var(--tami-robot-size) * 0.1);
        border-radius: 999px;
        background: linear-gradient(180deg, var(--tami-robot-accent), #ffe8a1);
        box-shadow: 0 0 0.24rem rgba(255, 209, 102, 0.48);
      }

      .tami-robot__neck {
        position: absolute;
        left: 50%;
        bottom: calc(var(--tami-robot-size) * 0.48);
        z-index: 2;
        width: calc(var(--tami-robot-size) * 0.16);
        height: calc(var(--tami-robot-size) * 0.16);
        border-radius: 0.2rem;
        background: linear-gradient(180deg, #d8f0ee, #b8dedb);
        transform: translateX(-50%);
      }

      .tami-robot__arm {
        position: absolute;
        top: calc(var(--tami-robot-size) * 0.76);
        z-index: 0;
        width: calc(var(--tami-robot-size) * 0.15);
        height: calc(var(--tami-robot-size) * 0.48);
        border: 2px solid var(--tami-robot-line);
        border-radius: 999px;
        background: linear-gradient(180deg, #ffffff, #dff3f1);
        transform-origin: 50% 8%;
      }

      .tami-robot__arm::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: -0.07rem;
        width: calc(var(--tami-robot-size) * 0.14);
        height: calc(var(--tami-robot-size) * 0.14);
        border-radius: 999px;
        background: linear-gradient(180deg, #ffd166, #ffe5a0);
        transform: translateX(-50%);
      }

      .tami-robot__arm--left {
        left: calc(50% - var(--tami-robot-size) * 0.48);
        transform: rotate(18deg);
      }

      .tami-robot__arm--right {
        right: calc(50% - var(--tami-robot-size) * 0.48);
        transform: rotate(-18deg);
      }

      .tami-robot__head {
        position: absolute;
        top: calc(var(--tami-robot-size) * 0.07);
        left: 50%;
        z-index: 4;
        width: calc(var(--tami-robot-size) * 0.88);
        height: calc(var(--tami-robot-size) * 0.75);
        border: 2px solid var(--tami-robot-line);
        border-radius: 40% 40% 34% 34%;
        background:
          radial-gradient(circle at 25% 20%, rgba(255, 255, 255, 0.98) 0 13%, transparent 14%),
          linear-gradient(135deg, var(--tami-robot-face), #ffffff 72%);
        box-shadow: 0 0.36rem 0.86rem rgba(36, 122, 118, 0.2);
        transform: translateX(-50%);
        transform-origin: center 78%;
      }

      .tami-robot__head::before {
        content: '';
        position: absolute;
        inset: 13% 10% 18% 10%;
        border-radius: 36% 36% 28% 28%;
        background: linear-gradient(180deg, rgba(52, 169, 161, 0.09), rgba(52, 169, 161, 0.02));
      }

      .tami-robot__cap {
        position: absolute;
        top: calc(var(--tami-robot-size) * -0.08);
        left: 50%;
        width: calc(var(--tami-robot-size) * 0.66);
        height: calc(var(--tami-robot-size) * 0.22);
        border-radius: 999px 999px 0.45rem 0.45rem;
        background: linear-gradient(180deg, #b7eeea, var(--tami-robot-primary));
        transform: translateX(-50%);
      }

      .tami-robot__cap::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: -0.08rem;
        width: 84%;
        height: 0.12rem;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.92);
        transform: translateX(-50%);
      }

      .tami-robot__antenna {
        position: absolute;
        top: calc(var(--tami-robot-size) * -0.2);
        left: 50%;
        width: 2px;
        height: calc(var(--tami-robot-size) * 0.22);
        background: var(--tami-robot-line);
        transform: translateX(-50%);
      }

      .tami-robot__antenna::before {
        content: '';
        position: absolute;
        top: -0.28rem;
        left: 50%;
        width: 0.38rem;
        height: 0.38rem;
        border-radius: 999px;
        background: linear-gradient(135deg, var(--tami-robot-accent), #fff0a8);
        transform: translateX(-50%);
        box-shadow: 0 0 0.55rem rgba(255, 209, 102, 0.62);
      }

      .tami-robot__ear {
        position: absolute;
        top: 35%;
        width: calc(var(--tami-robot-size) * 0.12);
        height: calc(var(--tami-robot-size) * 0.17);
        border: 2px solid var(--tami-robot-line);
        border-radius: 999px;
        background: linear-gradient(180deg, #ffffff, #ddf2f0);
      }

      .tami-robot__ear--left { left: calc(var(--tami-robot-size) * -0.06); }
      .tami-robot__ear--right { right: calc(var(--tami-robot-size) * -0.06); }

      .tami-robot__eye {
        position: absolute;
        top: 38%;
        width: calc(var(--tami-robot-size) * 0.12);
        height: calc(var(--tami-robot-size) * 0.14);
        border-radius: 999px;
        background: var(--tami-robot-eye);
        box-shadow: 0 0 0.36rem rgba(36, 122, 118, 0.38);
      }

      .tami-robot__eye--left { left: 28%; }
      .tami-robot__eye--right { right: 28%; }

      .tami-robot__cheek {
        position: absolute;
        top: 55%;
        width: calc(var(--tami-robot-size) * 0.12);
        height: calc(var(--tami-robot-size) * 0.06);
        border-radius: 999px;
        background: rgba(233, 95, 138, 0.28);
      }

      .tami-robot__cheek--left { left: 21%; }
      .tami-robot__cheek--right { right: 21%; }

      .tami-robot__mouth {
        position: absolute;
        left: 50%;
        bottom: 18%;
        width: calc(var(--tami-robot-size) * 0.26);
        height: calc(var(--tami-robot-size) * 0.055);
        border: 2px solid var(--tami-robot-mouth);
        border-top: 0;
        border-radius: 0 0 999px 999px;
        background: rgba(233, 95, 138, 0.1);
        transform: translateX(-50%);
        transform-origin: center top;
        overflow: hidden;
      }

      .tami-robot__spark {
        position: absolute;
        top: calc(var(--tami-robot-size) * 0.02);
        width: calc(var(--tami-robot-size) * 0.11);
        height: calc(var(--tami-robot-size) * 0.11);
        opacity: 0;
      }

      .tami-robot__spark::before,
      .tami-robot__spark::after {
        content: '';
        position: absolute;
        inset: 0;
        border-radius: 999px;
        background: linear-gradient(180deg, rgba(255, 209, 102, 0.96), rgba(255, 209, 102, 0.12));
      }

      .tami-robot__spark::after { transform: rotate(90deg); }
      .tami-robot__spark--left { left: 2%; }
      .tami-robot__spark--right { right: 2%; }

      .tami-robot--hover:hover .tami-robot__head,
      .tami-robot--hover:focus-within .tami-robot__head {
        animation: tamiRobotGreetingHead 1.05s ease-in-out;
      }

      .tami-robot--hover:hover .tami-robot__body,
      .tami-robot--hover:focus-within .tami-robot__body {
        animation: tamiRobotGreetingBody 1.05s ease-in-out;
      }

      .tami-robot--hover:hover .tami-robot__arm--right,
      .tami-robot--hover:focus-within .tami-robot__arm--right {
        animation: tamiRobotWave 1.05s ease-in-out;
      }

      .tami-robot--hover:hover .tami-robot__spark,
      .tami-robot--hover:focus-within .tami-robot__spark {
        animation: tamiRobotSparkle 1.05s ease-in-out;
      }

      .tami-robot--speaking {
        animation: tamiRobotSpeechBob 0.52s ease-in-out infinite;
      }

      .tami-robot--speaking .tami-robot__head {
        animation: tamiRobotSpeakHead 0.34s ease-in-out infinite;
      }

      .tami-robot--speaking .tami-robot__mouth {
        animation-name: tamiRobotTalk;
        animation-duration: var(--tami-mouth-animation-duration, 170ms);
        animation-timing-function: ease-in-out;
        animation-iteration-count: infinite;
      }

      .tami-robot--speaking .tami-robot__eye {
        animation: tamiRobotBlink 1.05s ease-in-out infinite;
      }

      @keyframes tamiRobotGreetingHead {
        0%, 100% { transform: translateX(-50%) rotate(0deg) translateY(0); }
        18% { transform: translateX(-50%) rotate(-8deg) translateY(-2px); }
        42% { transform: translateX(-50%) rotate(8deg) translateY(-1px); }
        66% { transform: translateX(-50%) rotate(-5deg) translateY(0); }
      }

      @keyframes tamiRobotGreetingBody {
        0%, 100% { transform: translateX(-50%) translateY(0); }
        32% { transform: translateX(-50%) translateY(-2px); }
        64% { transform: translateX(-50%) translateY(1px); }
      }

      @keyframes tamiRobotWave {
        0%, 100% { transform: rotate(-18deg); }
        16% { transform: rotate(-62deg); }
        32% { transform: rotate(-20deg); }
        50% { transform: rotate(-68deg); }
        68% { transform: rotate(-24deg); }
      }

      @keyframes tamiRobotSparkle {
        0%, 100% { opacity: 0; transform: scale(0.4) rotate(0deg); }
        25% { opacity: 1; transform: scale(1.08) rotate(18deg); }
        60% { opacity: 0.76; transform: scale(0.86) rotate(-12deg); }
      }

      @keyframes tamiRobotSpeechBob {
        0%, 100% { transform: translateY(0); }
        50% { transform: translateY(-1px); }
      }

      @keyframes tamiRobotSpeakHead {
        0%, 100% { transform: translateX(-50%) rotate(0deg); }
        50% { transform: translateX(-50%) rotate(2deg); }
      }

      @keyframes tamiRobotTalk {
        0%, 100% {
          width: calc(var(--tami-robot-size) * 0.24);
          height: calc(var(--tami-robot-size) * 0.06);
          border-radius: 0 0 999px 999px;
          transform: translateX(-50%) scaleY(1);
        }
        35% {
          width: calc(var(--tami-robot-size) * 0.18);
          height: calc(var(--tami-robot-size) * 0.18);
          border-radius: 0.42rem;
          transform: translateX(-50%) scaleY(1.1);
        }
        66% {
          width: calc(var(--tami-robot-size) * 0.22);
          height: calc(var(--tami-robot-size) * 0.24);
          border-radius: 999px;
          transform: translateX(-50%) scaleY(1.16);
        }
      }

      @keyframes tamiRobotBlink {
        0%, 89%, 100% { transform: scaleY(1); }
        93% { transform: scaleY(0.15); }
      }
    `
  ]
})
export class TamiRobotComponent {
  @Input() size: TamiRobotSize = 'md';
  @Input() mode: TamiRobotMode = 'idle';
  @Input() mouthAnimationDurationMs = 170;

  get robotClasses(): string[] {
    return [`tami-robot--${this.size}`, `tami-robot--${this.mode}`];
  }
}
