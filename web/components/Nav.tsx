"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/", label: "Create" },
  { href: "/lookup", label: "Look up" },
  { href: "/members", label: "Members" },
  { href: "/how-it-works", label: "How it works" },
];

export const REPO_URL = "https://github.com/devowl-teguh/java_springboot_microservice";

export default function Nav() {
  return <NavShell path={usePathname()} />;
}

/** Without a path (the Suspense fallback) no link is marked active. */
export function NavShell({ path }: { path?: string }) {
  return (
    <header className="border-b border-line bg-panel">
      <nav className="mx-auto flex max-w-3xl flex-wrap items-center gap-x-5 gap-y-2 px-4 py-3 text-sm sm:px-6">
        <Link href="/" className="mr-2 font-semibold text-fg">
          Transaction demo
        </Link>
        {links.map((l) => {
          const active = path !== undefined && (l.href === "/" ? path === "/" : path.startsWith(l.href));
          return (
            <Link
              key={l.href}
              href={l.href}
              aria-current={active ? "page" : undefined}
              className={active ? "font-medium text-fg" : "text-muted hover:text-fg"}
            >
              {l.label}
            </Link>
          );
        })}
        <a href={REPO_URL} target="_blank" rel="noreferrer" className="ml-auto text-muted hover:text-fg">
          GitHub ↗
        </a>
      </nav>
    </header>
  );
}
