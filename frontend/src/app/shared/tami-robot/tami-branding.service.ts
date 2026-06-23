import { DOCUMENT } from '@angular/common';
import { Injectable, NgZone, inject } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TamiBrandingService {
  private readonly document = inject(DOCUMENT);
  private readonly ngZone = inject(NgZone);

  private observer?: MutationObserver;
  private scheduled = false;
  private started = false;

  private readonly styles = `
    .tami-robot-shell {
      --tami-robot-size: 1.8rem;
      --tami-robot-primary: #34a9a1;
      --tami-robot-primary-dark: #247a76;
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
      transform-origin: center bottom;
      overflow: visible;
    }

    .tami-robot-shell--sm { --tami-robot-size: 1.52rem; }
    .tami-robot-shell--md { --tami-robot-size: 2rem; }
    .tami-robot-shell--lg { --tami-robot-size: 2.7rem; }

    .tami-robot-shell--head-only {
      width: calc(var(--tami-robot-size) * 1.05);
      height: calc(var(--tami-robot-size) * 0.98);
      min-width: calc(var(--tami-robot-size) * 1.05);
      align-items: center;
    }

    .tami-robot-body {
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

    .tami-robot-body::before {
      content: '';
      position: absolute;
      top: 17%;
      left: 15%;
      right: 15%;
      height: 17%;
      border-radius: 999px;
      background: rgba(52, 169, 161, 0.14);
    }

    .tami-robot-body::after {
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

    .tami-robot-badge {
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

    .tami-robot-neck {
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

    .tami-robot-arm {
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

    .tami-robot-arm::after {
      content: '';
      position: absolute;
      left: 50%;
      bottom: -0.07rem;
      width: calc(var(--tami-robot-size) * 0.14);
      height: calc(var(--tami-robot-size) * 0.14);
      border-radius: 999px;
      background: linear-gradient(180deg, #ffd166, #ffe5a0);
      transform: translateX(-50%);
      box-shadow: 0 0 0.22rem rgba(255, 209, 102, 0.38);
    }

    .tami-robot-arm--left {
      left: calc(50% - var(--tami-robot-size) * 0.48);
      transform: rotate(18deg);
    }

    .tami-robot-arm--right {
      right: calc(50% - var(--tami-robot-size) * 0.48);
      transform: rotate(-18deg);
    }

    .tami-robot-head {
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

    .tami-robot-shell--head-only .tami-robot-head {
      top: 50%;
      transform: translate(-50%, -50%);
    }

    .tami-robot-head::before {
      content: '';
      position: absolute;
      inset: 13% 10% 18% 10%;
      border-radius: 36% 36% 28% 28%;
      background: linear-gradient(180deg, rgba(52, 169, 161, 0.09), rgba(52, 169, 161, 0.02));
    }

    .tami-robot-cap {
      position: absolute;
      top: calc(var(--tami-robot-size) * -0.08);
      left: 50%;
      z-index: 2;
      width: calc(var(--tami-robot-size) * 0.66);
      height: calc(var(--tami-robot-size) * 0.22);
      border-radius: 999px 999px 0.45rem 0.45rem;
      background: linear-gradient(180deg, #b7eeea, var(--tami-robot-primary));
      transform: translateX(-50%);
      box-shadow: inset 0 -1px 0 rgba(255, 255, 255, 0.36);
    }

    .tami-robot-cap::after {
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

    .tami-robot-antenna {
      position: absolute;
      top: calc(var(--tami-robot-size) * -0.2);
      left: 50%;
      width: 2px;
      height: calc(var(--tami-robot-size) * 0.22);
      background: var(--tami-robot-line);
      transform: translateX(-50%);
    }

    .tami-robot-antenna::before {
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

    .tami-robot-ear {
      position: absolute;
      top: 35%;
      width: calc(var(--tami-robot-size) * 0.12);
      height: calc(var(--tami-robot-size) * 0.17);
      border: 2px solid var(--tami-robot-line);
      border-radius: 999px;
      background: linear-gradient(180deg, #ffffff, #ddf2f0);
    }

    .tami-robot-ear--left { left: calc(var(--tami-robot-size) * -0.06); }
    .tami-robot-ear--right { right: calc(var(--tami-robot-size) * -0.06); }

    .tami-robot-eye {
      position: absolute;
      top: 38%;
      width: calc(var(--tami-robot-size) * 0.12);
      height: calc(var(--tami-robot-size) * 0.14);
      border-radius: 999px;
      background: var(--tami-robot-eye);
      box-shadow: 0 0 0.36rem rgba(36, 122, 118, 0.38);
      transform-origin: center;
    }

    .tami-robot-eye--left { left: 28%; }
    .tami-robot-eye--right { right: 28%; }

    .tami-robot-cheek {
      position: absolute;
      top: 55%;
      width: calc(var(--tami-robot-size) * 0.12);
      height: calc(var(--tami-robot-size) * 0.06);
      border-radius: 999px;
      background: rgba(233, 95, 138, 0.28);
    }

    .tami-robot-cheek--left { left: 21%; }
    .tami-robot-cheek--right { right: 21%; }

    .tami-robot-mouth {
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

    .tami-robot-mouth::after {
      content: '';
      position: absolute;
      left: 50%;
      bottom: 0;
      width: 64%;
      height: 46%;
      border-radius: 999px 999px 0 0;
      background: rgba(233, 95, 138, 0.34);
      transform: translateX(-50%);
    }

    .tami-robot-spark {
      position: absolute;
      top: calc(var(--tami-robot-size) * 0.02);
      width: calc(var(--tami-robot-size) * 0.11);
      height: calc(var(--tami-robot-size) * 0.11);
      opacity: 0;
      z-index: 5;
    }

    .tami-robot-spark::before,
    .tami-robot-spark::after {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: 999px;
      background: linear-gradient(180deg, rgba(255, 209, 102, 0.96), rgba(255, 209, 102, 0.12));
    }

    .tami-robot-spark::after { transform: rotate(90deg); }
    .tami-robot-spark--left { left: 2%; }
    .tami-robot-spark--right { right: 2%; }

    .tami-robot-shell--head-only .tami-robot-body,
    .tami-robot-shell--head-only .tami-robot-badge,
    .tami-robot-shell--head-only .tami-robot-neck,
    .tami-robot-shell--head-only .tami-robot-arm {
      display: none;
    }

    .tami-robot-shell--head-only .tami-robot-spark {
      top: 12%;
    }

    .tami-nav-link { gap: 0.55rem; }

    .tami-nav-link .tami-robot-shell {
      margin-left: -0.08rem;
      margin-right: -0.12rem;
    }

    .tami-nav-link:hover .tami-robot-head,
    .tami-nav-link:focus-visible .tami-robot-head {
      animation: tamiRobotGreetingHead 1.05s ease-in-out;
    }

    .tami-nav-link:hover .tami-robot-shell--head-only .tami-robot-head,
    .tami-nav-link:focus-visible .tami-robot-shell--head-only .tami-robot-head {
      animation: tamiRobotGreetingHeadOnly 1.05s ease-in-out;
    }

    .tami-nav-link:hover .tami-robot-body,
    .tami-nav-link:focus-visible .tami-robot-body {
      animation: tamiRobotGreetingBody 1.05s ease-in-out;
    }

    .tami-nav-link:hover .tami-robot-arm--right,
    .tami-nav-link:focus-visible .tami-robot-arm--right {
      animation: tamiRobotWave 1.05s ease-in-out;
    }

    .tami-nav-link:hover .tami-robot-antenna::before,
    .tami-nav-link:focus-visible .tami-robot-antenna::before,
    body.tami-is-speaking .tami-robot-session-title .tami-robot-antenna::before {
      animation: tamiRobotGlow 0.65s ease-in-out infinite;
    }

    .tami-nav-link:hover .tami-robot-spark,
    .tami-nav-link:focus-visible .tami-robot-spark {
      animation: tamiRobotSparkle 1.05s ease-in-out;
    }

    .tami-title-enhanced,
    .tami-session-title-enhanced {
      display: flex !important;
      align-items: center;
      gap: 0.65rem;
    }

    .tami-session-title-enhanced .tami-robot-shell {
      --tami-robot-size: 1.95rem;
    }

    body.tami-is-speaking .tami-robot-session-title {
      animation: tamiRobotSpeechBob 0.52s ease-in-out infinite;
    }

    body.tami-is-speaking .tami-robot-session-title .tami-robot-head {
      animation: tamiRobotSpeakHead 0.34s ease-in-out infinite;
    }

    body.tami-is-speaking .tami-robot-session-title .tami-robot-mouth {
      animation: tamiRobotTalk 0.17s ease-in-out infinite;
    }

    body.tami-is-speaking .tami-robot-session-title .tami-robot-eye {
      animation: tamiRobotBlink 1.05s ease-in-out infinite;
    }

    @keyframes tamiRobotGreetingHead {
      0%, 100% { transform: translateX(-50%) rotate(0deg) translateY(0); }
      18% { transform: translateX(-50%) rotate(-8deg) translateY(-2px); }
      42% { transform: translateX(-50%) rotate(8deg) translateY(-1px); }
      66% { transform: translateX(-50%) rotate(-5deg) translateY(0); }
    }

    @keyframes tamiRobotGreetingHeadOnly {
      0%, 100% { transform: translate(-50%, -50%) rotate(0deg) translateY(0); }
      18% { transform: translate(-50%, -50%) rotate(-8deg) translateY(-2px); }
      42% { transform: translate(-50%, -50%) rotate(8deg) translateY(-1px); }
      66% { transform: translate(-50%, -50%) rotate(-5deg) translateY(0); }
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

    @keyframes tamiRobotGlow {
      0%, 100% { box-shadow: 0 0 0.55rem rgba(255, 209, 102, 0.62); }
      50% { box-shadow: 0 0 1rem rgba(52, 169, 161, 0.55); }
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
  `;

  start(): void {
    if (this.started || typeof MutationObserver === 'undefined') {
      return;
    }

    this.started = true;
    this.injectStyles();

    this.ngZone.runOutsideAngular(() => {
      window.setTimeout(() => this.enhanceAll(), 0);
      this.observer = new MutationObserver(() => this.scheduleEnhance());
      this.observer.observe(this.document.body, {
        childList: true,
        subtree: true,
        characterData: true
      });
    });
  }

  private scheduleEnhance(): void {
    if (this.scheduled) {
      return;
    }

    this.scheduled = true;
    window.setTimeout(() => {
      this.scheduled = false;
      this.enhanceAll();
    }, 80);
  }

  private enhanceAll(): void {
    this.enhanceAiNavigationLinks();
    this.enhanceAiPageTitle();
    this.enhanceAiSessionTitle();
    this.updateSpeakingState();
  }

  private enhanceAiNavigationLinks(): void {
    const links = Array.from(this.document.querySelectorAll<HTMLAnchorElement>('a[href$="/ai-assistant"]'));

    links.forEach((link) => {
      link.classList.add('tami-nav-link');

      const existingRobot = link.querySelector<HTMLElement>('.tami-robot-shell');
      if (existingRobot) {
        this.ensureHeadOnlyRobot(existingRobot);
        return;
      }

      const robot = this.createRobotElement('tami-robot-shell--sm tami-robot-nav tami-robot-shell--head-only', true);
      const icon = link.querySelector<HTMLElement>('i.bi, .bi, svg');

      if (icon) {
        icon.after(robot);
      } else {
        link.prepend(robot);
      }
    });
  }

  private enhanceAiPageTitle(): void {
    if (!this.isAiAssistantRoute()) {
      return;
    }

    const title = Array.from(this.document.querySelectorAll<HTMLElement>('h1'))
      .find((heading) => this.normalizeText(heading.textContent).startsWith('tami'));

    if (!title || title.querySelector('.tami-robot-shell')) {
      return;
    }

    title.classList.add('tami-title-enhanced');
    title.prepend(this.createRobotElement('tami-robot-shell--lg tami-robot-page-title', false));
  }

  private enhanceAiSessionTitle(): void {
    if (!this.isAiAssistantRoute()) {
      return;
    }

    this.removeMisplacedSessionRobots();

    const sessionTitle = Array.from(this.document.querySelectorAll<HTMLElement>('h2'))
      .find((heading) => this.isActiveSessionHeading(heading));

    if (!sessionTitle || sessionTitle.querySelector('.tami-robot-shell')) {
      return;
    }

    sessionTitle.classList.add('tami-session-title-enhanced');
    sessionTitle.prepend(this.createRobotElement('tami-robot-shell--md tami-robot-session-title', false));
  }

  private removeMisplacedSessionRobots(): void {
    const headings = Array.from(this.document.querySelectorAll<HTMLElement>('h2.tami-session-title-enhanced'));

    headings
      .filter((heading) => !this.isActiveSessionHeading(heading))
      .forEach((heading) => {
        heading.querySelector('.tami-robot-session-title')?.remove();
        heading.classList.remove('tami-session-title-enhanced');
      });
  }

  private isActiveSessionHeading(heading: HTMLElement): boolean {
    const text = this.normalizeText(heading.textContent);

    if (!text || this.isSessionsListHeadingText(text) || heading.closest('.list-group')) {
      return false;
    }

    const container = heading.parentElement;
    const containerText = this.normalizeText(container?.textContent ?? '');

    return (
      containerText.includes('chat') ||
      containerText.includes('search') ||
      containerText.includes('buscar') ||
      containerText.includes('top k') ||
      containerText.includes('threshold') ||
      containerText.includes('umbral') ||
      text.includes('new session') ||
      text.includes('nueva sesion') ||
      text.includes('sesion nueva')
    );
  }

  private isSessionsListHeadingText(text: string): boolean {
    return [
      'sessions',
      'sesiones',
      'chat sessions',
      'sesiones de chat',
      'historial de sesiones'
    ].includes(text);
  }

  private updateSpeakingState(): void {
    const isTyping = this.isAiAssistantRoute() && Boolean(this.document.querySelector('.ai-typing-cursor'));
    this.document.body.classList.toggle('tami-is-speaking', isTyping);
  }

  private createRobotElement(extraClasses: string, headOnly: boolean): HTMLElement {
    const robot = this.document.createElement('span');
    robot.className = `tami-robot-shell ${extraClasses}`;
    robot.setAttribute('aria-hidden', 'true');
    robot.innerHTML = this.robotTemplate();

    if (headOnly) {
      this.ensureHeadOnlyRobot(robot);
    }

    return robot;
  }

  private ensureHeadOnlyRobot(robot: HTMLElement): void {
    robot.classList.add('tami-robot-shell--head-only');
    robot.querySelectorAll('.tami-robot-body, .tami-robot-badge, .tami-robot-neck, .tami-robot-arm')
      .forEach((element) => element.remove());
  }

  private robotTemplate(): string {
    return `
      <span class="tami-robot-spark tami-robot-spark--left"></span>
      <span class="tami-robot-spark tami-robot-spark--right"></span>
      <span class="tami-robot-arm tami-robot-arm--left"></span>
      <span class="tami-robot-arm tami-robot-arm--right"></span>
      <span class="tami-robot-body"></span>
      <span class="tami-robot-badge"></span>
      <span class="tami-robot-neck"></span>
      <span class="tami-robot-head">
        <span class="tami-robot-antenna"></span>
        <span class="tami-robot-cap"></span>
        <span class="tami-robot-ear tami-robot-ear--left"></span>
        <span class="tami-robot-ear tami-robot-ear--right"></span>
        <span class="tami-robot-cheek tami-robot-cheek--left"></span>
        <span class="tami-robot-cheek tami-robot-cheek--right"></span>
        <span class="tami-robot-eye tami-robot-eye--left"></span>
        <span class="tami-robot-eye tami-robot-eye--right"></span>
        <span class="tami-robot-mouth"></span>
      </span>
    `;
  }

  private isAiAssistantRoute(): boolean {
    return this.document.location.pathname.includes('/ai-assistant');
  }

  private normalizeText(value: string | null): string {
    return this.normalizeWhitespace(value ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }

  private normalizeWhitespace(value: string): string {
    return value.replace(/\s+/g, ' ').trim();
  }

  private injectStyles(): void {
    if (this.document.getElementById('tamias-tami-branding-styles')) {
      return;
    }

    const style = this.document.createElement('style');
    style.id = 'tamias-tami-branding-styles';
    style.textContent = this.styles;
    this.document.head.appendChild(style);
  }
}
