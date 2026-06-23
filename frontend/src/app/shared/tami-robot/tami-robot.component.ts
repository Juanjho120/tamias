import { NgClass } from '@angular/common';
import { Component, Input } from '@angular/core';

export type TamiRobotSize = 'sm' | 'md' | 'lg';
export type TamiRobotMode = 'idle' | 'hover' | 'speaking';

@Component({
  selector: 'app-tami-robot',
  standalone: true,
  imports: [NgClass],
  template: `
    <span class="tami-robot" [ngClass]="robotClasses" aria-hidden="true">
      <span class="tami-robot__spark tami-robot__spark--left"></span>
      <span class="tami-robot__spark tami-robot__spark--right"></span>

      <span class="tami-robot__arm tami-robot__arm--left"></span>
      <span class="tami-robot__arm tami-robot__arm--right"></span>

      <span class="tami-robot__body">
        <span class="tami-robot__body-panel"></span>
        <span class="tami-robot__apron"></span>
        <span class="tami-robot__badge"></span>
      </span>

      <span class="tami-robot__neck"></span>

      <span class="tami-robot__head">
        <span class="tami-robot__antenna"></span>
        <span class="tami-robot__cap"></span>
        <span class="tami-robot__ear tami-robot__ear--left"></span>
        <span class="tami-robot__ear tami-robot__ear--right"></span>
        <span class="tami-robot__faceplate"></span>
        <span class="tami-robot__cheek tami-robot__cheek--left"></span>
        <span class="tami-robot__cheek tami-robot__cheek--right"></span>
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
        --tami-robot-primary: #39a7a1;
        --tami-robot-primary-dark: #2d7f7b;
        --tami-robot-accent: #ffd166;
        --tami-robot-face: #f7fbfb;
        --tami-robot-line: rgba(45, 127, 123, 0.42);
        --tami-robot-eye: #2d7f7b;
        --tami-robot-mouth: #eb6f92;
        --tami-robot-apron: #e5f6f5;
        --tami-robot-shadow: rgba(25, 64, 61, 0.18);
        position: relative;
        display: inline-flex;
        align-items: flex-end;
        justify-content: center;
        width: calc(var(--tami-robot-size) * 1.12);
        height: calc(var(--tami-robot-size) * 1.28);
        min-width: calc(var(--tami-robot-size) * 1.12);
        vertical-align: middle;
        transform-origin: center bottom;
      }

      .tami-robot--sm {
        --tami-robot-size: 1.65rem;
      }

      .tami-robot--lg {
        --tami-robot-size: 2.75rem;
      }

      .tami-robot__head {
        position: absolute;
        top: calc(var(--tami-robot-size) * 0.06);
        left: 50%;
        z-index: 3;
        width: calc(var(--tami-robot-size) * 0.86);
        height: calc(var(--tami-robot-size) * 0.74);
        border: 2px solid var(--tami-robot-line);
        border-radius: 40% 40% 36% 36%;
        background:
          radial-gradient(circle at 25% 20%, rgba(255, 255, 255, 0.96) 0 12%, transparent 13%),
          linear-gradient(135deg, var(--tami-robot-face), #ffffff 72%);
        box-shadow: 0 0.38rem 0.9rem rgba(45, 127, 123, 0.18);
        transform: translateX(-50%);
        transform-origin: center 80%;
      }

      .tami-robot__faceplate {
        position: absolute;
        inset: 12% 10% 18% 10%;
        border-radius: 36% 36% 30% 30%;
        background: linear-gradient(180deg, rgba(57, 167, 161, 0.08), rgba(57, 167, 161, 0.02));
      }

      .tami-robot__antenna {
        position: absolute;
        top: calc(var(--tami-robot-size) * -0.18);
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
        background: linear-gradient(135deg, var(--tami-robot-accent), #ffef9f);
        transform: translateX(-50%);
        box-shadow: 0 0 0.52rem rgba(255, 209, 102, 0.55);
      }

      .tami-robot__cap {
        position: absolute;
        top: calc(var(--tami-robot-size) * -0.04);
        left: 50%;
        width: calc(var(--tami-robot-size) * 0.62);
        height: calc(var(--tami-robot-size) * 0.18);
        border-radius: 999px 999px 0.45rem 0.45rem;
        background: linear-gradient(180deg, #a7e7e3, var(--tami-robot-primary));
        transform: translateX(-50%);
        box-shadow: inset 0 -1px 0 rgba(255, 255, 255, 0.28);
      }

      .tami-robot__ear {
        position: absolute;
        top: 36%;
        width: calc(var(--tami-robot-size) * 0.12);
        height: calc(var(--tami-robot-size) * 0.16);
        border: 2px solid var(--tami-robot-line);
        border-radius: 999px;
        background: linear-gradient(180deg, #ffffff, #dff3f1);
      }

      .tami-robot__ear--left {
        left: calc(var(--tami-robot-size) * -0.06);
      }

      .tami-robot__ear--right {
        right: calc(var(--tami-robot-size) * -0.06);
      }

      .tami-robot__eye {
        position: absolute;
        top: 37%;
        width: calc(var(--tami-robot-size) * 0.12);
        height: calc(var(--tami-robot-size) * 0.14);
        border-radius: 999px;
        background: var(--tami-robot-eye);
        box-shadow: 0 0 0.36rem rgba(45, 127, 123, 0.32);
        transform-origin: center;
      }

      .tami-robot__eye--left {
        left: 28%;
      }

      .tami-robot__eye--right {
        right: 28%;
      }

      .tami-robot__cheek {
        position: absolute;
        top: 54%;
        width: calc(var(--tami-robot-size) * 0.12);
        height: calc(var(--tami-robot-size) * 0.06);
        border-radius: 999px;
        background: rgba(235, 111, 146, 0.28);
      }

      .tami-robot__cheek--left {
        left: 22%;
      }

      .tami-robot__cheek--right {
        right: 22%;
      }

      .tami-robot__mouth {
        position: absolute;
        left: 50%;
        bottom: 19%;
        width: calc(var(--tami-robot-size) * 0.22);
        height: calc(var(--tami-robot-size) * 0.06);
        border: 2px solid var(--tami-robot-mouth);
        border-top: 0;
        border-radius: 0 0 999px 999px;
        background: rgba(235, 111, 146, 0.1);
        transform: translateX(-50%);
        transform-origin: center top;
        overflow: hidden;
      }

      .tami-robot__mouth::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: 0;
        width: 62%;
        height: 48%;
        border-radius: 999px 999px 0 0;
        background: rgba(235, 111, 146, 0.35);
        transform: translateX(-50%);
      }

      .tami-robot__neck {
        position: absolute;
        top: calc(var(--tami-robot-size) * 0.73);
        left: 50%;
        z-index: 1;
        width: calc(var(--tami-robot-size) * 0.16);
        height: calc(var(--tami-robot-size) * 0.12);
        border-radius: 0.18rem;
        background: linear-gradient(180deg, #d8f0ee, #bddfdd);
        transform: translateX(-50%);
      }

      .tami-robot__body {
        position: absolute;
        bottom: calc(var(--tami-robot-size) * 0.02);
        left: 50%;
        z-index: 0;
        width: calc(var(--tami-robot-size) * 0.72);
        height: calc(var(--tami-robot-size) * 0.46);
        border: 2px solid var(--tami-robot-line);
        border-radius: 0.9rem 0.9rem 0.6rem 0.6rem;
        background: linear-gradient(180deg, #f5fbfb, #e8f6f5);
        box-shadow: 0 0.22rem 0.55rem var(--tami-robot-shadow);
        transform: translateX(-50%);
        overflow: hidden;
      }

      .tami-robot__body-panel {
        position: absolute;
        inset: 16% 16% auto 16%;
        height: 18%;
        border-radius: 999px;
        background: rgba(57, 167, 161, 0.12);
      }

      .tami-robot__apron {
        position: absolute;
        left: 50%;
        bottom: 8%;
        width: 56%;
        height: 62%;
        border-radius: 0.42rem 0.42rem 0.6rem 0.6rem;
        background: linear-gradient(180deg, #ffffff, var(--tami-robot-apron));
        border: 1px solid rgba(45, 127, 123, 0.16);
        transform: translateX(-50%);
      }

      .tami-robot__apron::before {
        content: '';
        position: absolute;
        top: -18%;
        left: 50%;
        width: 64%;
        height: 26%;
        border-radius: 999px;
        border: 1px solid rgba(45, 127, 123, 0.16);
        background: rgba(255, 255, 255, 0.92);
        transform: translateX(-50%);
      }

      .tami-robot__badge {
        position: absolute;
        top: 35%;
        right: 17%;
        width: calc(var(--tami-robot-size) * 0.08);
        height: calc(var(--tami-robot-size) * 0.08);
        border-radius: 999px;
        background: linear-gradient(180deg, var(--tami-robot-accent), #ffe9a6);
        box-shadow: 0 0 0.22rem rgba(255, 209, 102, 0.36);
      }

      .tami-robot__arm {
        position: absolute;
        top: calc(var(--tami-robot-size) * 0.82);
        width: calc(var(--tami-robot-size) * 0.16);
        height: calc(var(--tami-robot-size) * 0.42);
        border-radius: 999px;
        border: 2px solid var(--tami-robot-line);
        background: linear-gradient(180deg, #ffffff, #ddf1ef);
        transform-origin: top center;
        z-index: 0;
      }

      .tami-robot__arm::after {
        content: '';
        position: absolute;
        bottom: -0.06rem;
        left: 50%;
        width: calc(var(--tami-robot-size) * 0.12);
        height: calc(var(--tami-robot-size) * 0.12);
        border-radius: 999px;
        background: linear-gradient(180deg, #ffd166, #ffe39b);
        transform: translateX(-50%);
      }

      .tami-robot__arm--left {
        left: calc(50% - var(--tami-robot-size) * 0.44);
        transform: rotate(16deg);
      }

      .tami-robot__arm--right {
        right: calc(50% - var(--tami-robot-size) * 0.44);
        transform: rotate(-16deg);
      }

      .tami-robot__spark {
        position: absolute;
        top: calc(var(--tami-robot-size) * 0.05);
        width: calc(var(--tami-robot-size) * 0.09);
        height: calc(var(--tami-robot-size) * 0.09);
        opacity: 0;
      }

      .tami-robot__spark::before,
      .tami-robot__spark::after {
        content: '';
        position: absolute;
        inset: 0;
        background: linear-gradient(180deg, rgba(255, 209, 102, 0.95), rgba(255, 209, 102, 0.2));
        border-radius: 999px;
      }

      .tami-robot__spark::after {
        transform: rotate(90deg);
      }

      .tami-robot__spark--left {
        left: 4%;
      }

      .tami-robot__spark--right {
        right: 4%;
      }

      .tami-robot--hover:hover .tami-robot__head,
      .tami-robot--hover:focus-within .tami-robot__head {
        animation: tamiRobotGreetingHead 1s ease-in-out;
      }

      .tami-robot--hover:hover .tami-robot__arm--right,
      .tami-robot--hover:focus-within .tami-robot__arm--right {
        animation: tamiRobotWave 1s ease-in-out;
      }

      .tami-robot--hover:hover .tami-robot__body,
      .tami-robot--hover:focus-within .tami-robot__body {
        animation: tamiRobotGreetingBody 1s ease-in-out;
      }

      .tami-robot--hover:hover .tami-robot__antenna::before,
      .tami-robot--hover:focus-within .tami-robot__antenna::before {
        animation: tamiRobotGlow 1s ease-in-out;
      }

      .tami-robot--hover:hover .tami-robot__spark,
      .tami-robot--hover:focus-within .tami-robot__spark {
        animation: tamiRobotSparkle 1s ease-in-out;
      }

      .tami-robot--speaking {
        animation: tamiRobotSpeechBob 0.55s ease-in-out infinite;
      }

      .tami-robot--speaking .tami-robot__mouth {
        animation: tamiRobotTalk 0.18s ease-in-out infinite;
      }

      .tami-robot--speaking .tami-robot__eye {
        animation: tamiRobotBlink 1.15s ease-in-out infinite;
      }

      .tami-robot--speaking .tami-robot__antenna::before {
        animation: tamiRobotGlow 0.6s ease-in-out infinite;
      }

      .tami-robot--speaking .tami-robot__head {
        animation: tamiRobotSpeakHead 0.34s ease-in-out infinite;
      }

      @keyframes tamiRobotGreetingHead {
        0%, 100% { transform: translateX(-50%) rotate(0deg) translateY(0); }
        20% { transform: translateX(-50%) rotate(-7deg) translateY(-2px); }
        45% { transform: translateX(-50%) rotate(7deg) translateY(-1px); }
        68% { transform: translateX(-50%) rotate(-5deg) translateY(0); }
      }

      @keyframes tamiRobotGreetingBody {
        0%, 100% { transform: translateX(-50%) translateY(0); }
        35% { transform: translateX(-50%) translateY(-1px); }
        65% { transform: translateX(-50%) translateY(1px); }
      }

      @keyframes tamiRobotWave {
        0%, 100% { transform: rotate(-16deg); }
        18% { transform: rotate(-54deg); }
        35% { transform: rotate(-22deg); }
        52% { transform: rotate(-58deg); }
        70% { transform: rotate(-26deg); }
      }

      @keyframes tamiRobotGlow {
        0%, 100% { box-shadow: 0 0 0.52rem rgba(255, 209, 102, 0.55); }
        50% { box-shadow: 0 0 1rem rgba(57, 167, 161, 0.48); }
      }

      @keyframes tamiRobotSparkle {
        0%, 100% { opacity: 0; transform: scale(0.5) rotate(0deg); }
        25% { opacity: 0.95; transform: scale(1) rotate(18deg); }
        60% { opacity: 0.7; transform: scale(0.88) rotate(-12deg); }
      }

      @keyframes tamiRobotSpeechBob {
        0%, 100% { transform: translateY(0); }
        50% { transform: translateY(-1px); }
      }

      @keyframes tamiRobotSpeakHead {
        0%, 100% { transform: translateX(-50%) rotate(0deg); }
        50% { transform: translateX(-50%) rotate(1.8deg); }
      }

      @keyframes tamiRobotTalk {
        0%, 100% {
          width: calc(var(--tami-robot-size) * 0.22);
          height: calc(var(--tami-robot-size) * 0.06);
          border-radius: 0 0 999px 999px;
          transform: translateX(-50%) scaleY(1);
        }
        35% {
          width: calc(var(--tami-robot-size) * 0.18);
          height: calc(var(--tami-robot-size) * 0.16);
          border-radius: 0.45rem;
          transform: translateX(-50%) scaleY(1.08);
        }
        65% {
          width: calc(var(--tami-robot-size) * 0.2);
          height: calc(var(--tami-robot-size) * 0.2);
          border-radius: 50%;
          transform: translateX(-50%) scaleY(1.15);
        }
      }

      @keyframes tamiRobotBlink {
        0%, 90%, 100% { transform: scaleY(1); }
        94% { transform: scaleY(0.16); }
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
