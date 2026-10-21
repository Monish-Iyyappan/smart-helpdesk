/**
 * Skiper UI Command Palette Component
 * Source: https://skiper-ui.com/components/command-palette
 * License: MIT
 * Adapted for Signal Desk tokens, glass styling, and motion/react.
 */

import React, { useEffect, useState, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { SearchIcon, XCircleIcon } from '../Icons';

export interface CommandItem {
  id: string;
  label: string;
  category: string;
  icon?: React.ReactNode;
  shortcut?: string;
  onSelect: () => void;
}

export interface CommandPaletteProps {
  isOpen: boolean;
  onClose: () => void;
  items: CommandItem[];
}

export const CommandPalette: React.FC<CommandPaletteProps> = ({
  isOpen,
  onClose,
  items,
}) => {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const filteredItems = items.filter((item) => {
    const q = query.toLowerCase();
    return item.label.toLowerCase().includes(q) || item.category.toLowerCase().includes(q);
  });

  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setSelectedIndex(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return;

      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex((prev) => (filteredItems.length ? (prev + 1) % filteredItems.length : 0));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex((prev) =>
          filteredItems.length ? (prev - 1 + filteredItems.length) % filteredItems.length : 0
        );
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (filteredItems[selectedIndex]) {
          filteredItems[selectedIndex].onSelect();
          onClose();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, filteredItems, selectedIndex, onClose]);

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-16 sm:pt-24 px-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm"
          />

          {/* Palette Dialog (Glass Blur) */}
          <motion.div
            initial={{ opacity: 0, scale: 0.96, y: -8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: -8 }}
            transition={{ type: 'spring', damping: 25, stiffness: 350 }}
            className="relative w-full max-w-lg rounded-2xl glass-panel shadow-2xl overflow-hidden border border-border"
            role="dialog"
            aria-modal="true"
            aria-label="Command Palette"
          >
            {/* Input Header */}
            <div className="flex items-center px-4 py-3.5 border-b border-border gap-3">
              <SearchIcon className="w-5 h-5 text-text-muted shrink-0" />
              <input
                ref={inputRef}
                type="text"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setSelectedIndex(0);
                }}
                placeholder="Type a command or navigate..."
                className="w-full bg-transparent text-sm text-text placeholder-text-muted outline-none"
              />
              <button
                type="button"
                onClick={onClose}
                className="p-1 rounded-md text-text-muted hover:text-text hover:bg-surface-hover transition-colors"
                aria-label="Close command palette"
              >
                <XCircleIcon className="w-4 h-4" />
              </button>
            </div>

            {/* Results List */}
            <div className="max-h-72 overflow-y-auto p-2 space-y-1">
              {filteredItems.length === 0 ? (
                <div className="p-6 text-center text-xs text-text-muted">
                  No commands found matching "{query}"
                </div>
              ) : (
                filteredItems.map((item, idx) => {
                  const isSelected = idx === selectedIndex;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => {
                        item.onSelect();
                        onClose();
                      }}
                      onMouseEnter={() => setSelectedIndex(idx)}
                      className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-medium transition-colors ${
                        isSelected
                          ? 'bg-primary text-primary-text font-semibold shadow-xs'
                          : 'text-text hover:bg-surface-hover'
                      }`}
                    >
                      <div className="flex items-center gap-2.5 truncate">
                        {item.icon && <span className="shrink-0">{item.icon}</span>}
                        <span className="truncate">{item.label}</span>
                        <span
                          className={`text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded ${
                            isSelected
                              ? 'bg-primary-hover text-primary-text'
                              : 'bg-surface-muted text-text-dim'
                          }`}
                        >
                          {item.category}
                        </span>
                      </div>
                      {item.shortcut && (
                        <kbd
                          className={`text-[10px] font-mono px-1.5 py-0.5 rounded border ${
                            isSelected
                              ? 'border-primary-hover bg-primary-hover text-primary-text'
                              : 'border-border bg-surface-muted text-text-muted'
                          }`}
                        >
                          {item.shortcut}
                        </kbd>
                      )}
                    </button>
                  );
                })
              )}
            </div>

            {/* Palette Footer */}
            <div className="px-4 py-2 border-t border-border bg-surface-muted/50 flex items-center justify-between text-[11px] text-text-muted">
              <div className="flex items-center gap-3">
                <span>↑↓ Navigate</span>
                <span>↵ Select</span>
                <span>ESC Close</span>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};
