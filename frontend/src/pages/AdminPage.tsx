import React, { useEffect, useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import type {
  AdminStats,
  ClosedTicketLogItem,
  IncidentItem,
  IncidentSeverity,
  TicketDetail,
  TicketItem,
  User,
} from '../api/types';
import { api } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { Shell } from '../components/Shell';
import { TicketCard } from '../components/TicketCard';
import { TicketDetailModal } from '../components/TicketDetailModal';
import { PlusIcon, UserPlusIcon, LinkIcon } from '../components/Icons';
import { CountUp } from '../components/CountUp';
import { AnimatedTabs } from '../components/ui/animated-tabs';
import { StatefulButton } from '../components/ui/stateful-button';
import { BentoStatCard } from '../components/BentoStatCard';
import { GreetingHeader } from '../components/GreetingHeader';
import { EmptyState } from '../components/EmptyState';
import { CardSkeleton } from '../components/ui/skeleton';

type AdminTab = 'STATS' | 'ALL_TICKETS' | 'UNASSIGNED' | 'OVERDUE' | 'INCIDENTS' | 'AGENTS' | 'CLOSED_LOG';

export const AdminPage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [activeTab, setActiveTab] = useState<AdminTab>('STATS');
  const [isLoading, setIsLoading] = useState(true);

  // Data states
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [allTickets, setAllTickets] = useState<TicketItem[]>([]);
  const [unassignedTickets, setUnassignedTickets] = useState<TicketItem[]>([]);
  const [overdueTickets, setOverdueTickets] = useState<TicketItem[]>([]);
  const [incidents, setIncidents] = useState<IncidentItem[]>([]);
  const [agents, setAgents] = useState<User[]>([]);
  const [closedLog, setClosedLog] = useState<ClosedTicketLogItem[]>([]);
  const [closedLogSearch, setClosedLogSearch] = useState('');

  // Filtering & Sorting for Tickets
  const [ticketSearch, setTicketSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState<'URGENCY' | 'NEWEST' | 'OLDEST'>('URGENCY');

  // Detail Modal
  const [selectedTicket, setSelectedTicket] = useState<TicketDetail | null>(null);

  // Modals
  const [showAddAgentModal, setShowAddAgentModal] = useState(false);
  const [showCreateIncidentModal, setShowCreateIncidentModal] = useState(false);
  const [showLinkIncidentModal, setShowLinkIncidentModal] = useState(false);

  // Add Agent Form
  const [agentName, setAgentName] = useState('');
  const [agentEmail, setAgentEmail] = useState('');
  const [agentPassword, setAgentPassword] = useState('');
  const [isAddingAgent, setIsAddingAgent] = useState(false);

  // Create Incident Form
  const [incidentTitle, setIncidentTitle] = useState('');
  const [incidentDescription, setIncidentDescription] = useState('');
  const [incidentLocation, setIncidentLocation] = useState('');
  const [incidentSeverity, setIncidentSeverity] = useState<IncidentSeverity>('MEDIUM');
  const [isCreatingIncident, setIsCreatingIncident] = useState(false);

  // Link Ticket to Incident Form
  const [linkIncidentId, setLinkIncidentId] = useState<number | ''>('');
  const [linkTicketId, setLinkTicketId] = useState<number | ''>('');
  const [isLinking, setIsLinking] = useState(false);

  const loadAllData = async () => {
    setIsLoading(true);
    try {
      const [sData, aTickets, uTickets, oTickets, incList, agList, cLog] = await Promise.all([
        api.admin.stats().catch(() => null),
        api.admin.tickets(),
        api.admin.unassignedTickets(),
        api.admin.overdueTickets(),
        api.incidents.list(),
        api.admin.listAgents(),
        api.admin.closedTickets().catch(() => []),
      ]);

      setStats(sData);
      setAllTickets(aTickets);
      setUnassignedTickets(uTickets);
      setOverdueTickets(oTickets);
      setIncidents(incList);
      setAgents(agList);
      setClosedLog(cLog || []);
    } catch {
      // Handled by global toast
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, []);

  const handleSelectTicket = async (ticket: TicketItem) => {
    try {
      const detail = await api.tickets.getByNumber(ticket.ticketNumber);
      setSelectedTicket(detail);
    } catch {
      // Handled by toast
    }
  };

  const handleRefreshSelected = async () => {
    if (selectedTicket) {
      try {
        const detail = await api.tickets.getByNumber(selectedTicket.ticketNumber);
        setSelectedTicket(detail);
      } catch {
        // Handled
      }
    }
    loadAllData();
    api.admin.closedTickets().then((cLog) => setClosedLog(cLog || [])).catch(() => {});
  };

  // Add Agent Submission
  const handleAddAgent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!agentName.trim() || !agentEmail.trim() || !agentPassword) return;

    setIsAddingAgent(true);
    try {
      await api.admin.createAgent({
        name: agentName.trim(),
        email: agentEmail.trim(),
        password: agentPassword,
      });
      showToast('Agent created successfully!', 'success');
      setShowAddAgentModal(false);
      setAgentName('');
      setAgentEmail('');
      setAgentPassword('');
      loadAllData();
    } catch {
      // Handled by global toast
    } finally {
      setIsAddingAgent(false);
    }
  };

  // Create Incident Submission
  const handleCreateIncident = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!incidentTitle.trim() || !incidentDescription.trim() || !incidentLocation.trim()) return;

    setIsCreatingIncident(true);
    try {
      await api.incidents.create({
        title: incidentTitle.trim(),
        description: incidentDescription.trim(),
        location: incidentLocation.trim(),
        severity: incidentSeverity,
      });
      showToast('Incident declared successfully!', 'success');
      setShowCreateIncidentModal(false);
      setIncidentTitle('');
      setIncidentDescription('');
      setIncidentLocation('');
      setIncidentSeverity('MEDIUM');
      loadAllData();
    } catch {
      // Handled by global toast
    } finally {
      setIsCreatingIncident(false);
    }
  };

  // Link Ticket Submission
  const handleLinkTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!linkIncidentId || !linkTicketId) return;

    setIsLinking(true);
    try {
      await api.admin.linkTicket(Number(linkIncidentId), Number(linkTicketId));
      showToast('Ticket linked to incident successfully!', 'success');
      setShowLinkIncidentModal(false);
      setLinkIncidentId('');
      setLinkTicketId('');
      loadAllData();
    } catch {
      // Handled by global toast
    } finally {
      setIsLinking(false);
    }
  };

  // Quick Inline Agent Assign
  const handleDirectAssign = async (ticketId: number, agentId: number) => {
    try {
      await api.admin.assignTicket(ticketId, agentId);
      showToast('Ticket assigned successfully', 'success');
      loadAllData();
    } catch {
      // Handled
    }
  };

  // Filtered & Sorted Tickets for the active list view
  const displayedTickets = useMemo(() => {
    let source: TicketItem[] = [];
    if (activeTab === 'ALL_TICKETS') source = allTickets;
    else if (activeTab === 'UNASSIGNED') source = unassignedTickets;
    else if (activeTab === 'OVERDUE') source = overdueTickets;
    else return [];

    let result = [...source];

    // Status filter
    if (statusFilter !== 'ALL') {
      result = result.filter((t) => t.status === statusFilter);
    }

    // Search query
    if (ticketSearch.trim()) {
      const q = ticketSearch.toLowerCase();
      result = result.filter(
        (t) =>
          t.ticketNumber.toLowerCase().includes(q) ||
          t.title.toLowerCase().includes(q) ||
          t.description.toLowerCase().includes(q) ||
          t.categoryName.toLowerCase().includes(q)
      );
    }

    // Sort
    if (sortBy === 'URGENCY') {
      const urgencyRank: Record<string, number> = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
      result.sort((a, b) => {
        const diff = (urgencyRank[b.urgencyLevel] || 0) - (urgencyRank[a.urgencyLevel] || 0);
        if (diff !== 0) return diff;
        return (b.urgencyScore || 0) - (a.urgencyScore || 0);
      });
    } else if (sortBy === 'OLDEST') {
      result.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    } else {
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }

    return result;
  }, [activeTab, allTickets, unassignedTickets, overdueTickets, statusFilter, ticketSearch, sortBy]);

  const adminTabs = [
    { id: 'STATS', label: 'Stats' },
    { id: 'ALL_TICKETS', label: 'All Tickets', count: allTickets.length },
    { id: 'UNASSIGNED', label: 'Unassigned', count: unassignedTickets.length },
    { id: 'OVERDUE', label: 'Overdue SLA', count: overdueTickets.length },
    { id: 'INCIDENTS', label: 'Incidents', count: incidents.length },
    { id: 'AGENTS', label: 'Agents', count: agents.length },
    { id: 'CLOSED_LOG', label: 'Closed Log', count: closedLog.length },
  ];

  const filteredClosedLog = useMemo(() => {
    if (!closedLogSearch.trim()) return closedLog;
    const q = closedLogSearch.toLowerCase();
    return closedLog.filter(
      (log) =>
        log.ticketNumber.toLowerCase().includes(q) ||
        log.title.toLowerCase().includes(q) ||
        log.category.toLowerCase().includes(q) ||
        (log.assignedAgentName && log.assignedAgentName.toLowerCase().includes(q)) ||
        log.closedBy.toLowerCase().includes(q) ||
        (log.closureNote && log.closureNote.toLowerCase().includes(q))
    );
  }, [closedLog, closedLogSearch]);

  return (
    <Shell
      title="Admin Center"
      tabs={adminTabs}
      activeTab={activeTab}
      onTabChange={(tabId) => setActiveTab(tabId as AdminTab)}
      headerActions={
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setShowAddAgentModal(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md border border-border bg-surface text-text hover:bg-surface-hover transition-colors"
          >
            <UserPlusIcon className="w-3.5 h-3.5" />
            <span>Add Agent</span>
          </button>
          <button
            type="button"
            onClick={() => setShowCreateIncidentModal(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md border border-border bg-surface text-text hover:bg-surface-hover transition-colors"
          >
            <PlusIcon className="w-3.5 h-3.5" />
            <span>New Incident</span>
          </button>
          <button
            type="button"
            onClick={() => setShowLinkIncidentModal(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md bg-primary text-primary-text hover:bg-primary-hover transition-colors shadow-xs"
          >
            <LinkIcon className="w-3.5 h-3.5" />
            <span>Link Ticket to Incident</span>
          </button>
        </div>
      }
    >
      {/* Bento Greeting Header */}
      <GreetingHeader
        subtitle="Full administrative control over tickets, SLA breaches, campus incidents, and agent workloads."
        actions={
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setShowAddAgentModal(true)}
              className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-semibold rounded-xl border border-border bg-surface text-text hover:bg-surface-hover transition-colors focus-ring"
            >
              <UserPlusIcon className="w-3.5 h-3.5" />
              <span>Add Agent</span>
            </button>
            <button
              type="button"
              onClick={() => setShowCreateIncidentModal(true)}
              className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-semibold rounded-xl border border-border bg-surface text-text hover:bg-surface-hover transition-colors focus-ring"
            >
              <PlusIcon className="w-3.5 h-3.5" />
              <span>New Incident</span>
            </button>
            <button
              type="button"
              onClick={() => setShowLinkIncidentModal(true)}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold rounded-xl bg-primary text-primary-text hover:bg-primary-hover transition-colors shadow-sm focus-ring"
            >
              <LinkIcon className="w-3.5 h-3.5" />
              <span>Link Ticket</span>
            </button>
          </div>
        }
      />

      {/* Aceternity Animated Tabs */}
      <div className="mb-6">
        <AnimatedTabs
          tabs={adminTabs}
          activeTab={activeTab}
          onTabChange={(id) => setActiveTab(id as AdminTab)}
        />
      </div>

      {/* Tab 1: Stats & System Overview */}
      {activeTab === 'STATS' && (
        <div className="space-y-6">
          {/* Key Metric Cards with BentoStatCards and Sparklines */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 min-w-0">
            <BentoStatCard
              label="Total Tickets"
              value={stats?.totalTickets || allTickets.length}
              sparklineData={[allTickets.length, allTickets.length + 2, allTickets.length + 1, allTickets.length]}
              description="System wide volume"
            />
            <BentoStatCard
              label="Unassigned Queue"
              value={unassignedTickets.length}
              sparklineData={[unassignedTickets.length + 1, unassignedTickets.length, unassignedTickets.length]}
              sparklineColor="var(--color-accent)"
              description="Requires agent triage"
            />
            <BentoStatCard
              label="Overdue SLA"
              value={stats?.overdueTickets ?? overdueTickets.length}
              sparklineData={[overdueTickets.length, overdueTickets.length + 1, overdueTickets.length]}
              isAlert={(stats?.overdueTickets ?? overdueTickets.length) > 0}
              description="Target SLA breached"
            />
            <BentoStatCard
              label="Active Incidents"
              value={stats?.totalIncidents ?? incidents.length}
              sparklineData={[incidents.length, incidents.length + 1, incidents.length]}
              sparklineColor="var(--color-danger)"
              description="Major campus issues"
            />
          </div>

          {/* Urgency Distribution with CountUp */}
          <div className="p-5 bg-surface border border-border rounded-xl shadow-xs">
            <h3 className="text-sm font-bold text-text mb-3">System Urgency Breakdown</h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {(['URGENT', 'HIGH', 'MEDIUM', 'LOW'] as const).map((level) => {
                const count = stats?.countsByUrgencyLevel?.[level] || 0;
                let textClasses = 'text-urgency-low-text bg-urgency-low-bg border-urgency-low-border';
                if (level === 'MEDIUM') textClasses = 'text-urgency-medium-text bg-urgency-medium-bg border-urgency-medium-border';
                if (level === 'HIGH') textClasses = 'text-urgency-high-text bg-urgency-high-bg border-urgency-high-border';
                if (level === 'URGENT') textClasses = 'text-urgency-urgent-text bg-urgency-urgent-bg border-urgency-urgent-border font-bold';

                return (
                  <div key={level} className={`p-3 rounded-lg border ${textClasses}`}>
                    <div className="text-xs uppercase tracking-wider">{level}</div>
                    <div className="text-xl font-bold mt-1">
                      <CountUp value={count} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Status Breakdown & User Distribution */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-5 bg-surface border border-border rounded-xl shadow-xs">
              <h3 className="text-sm font-bold text-text mb-3">Tickets by Status</h3>
              <div className="space-y-2">
                {stats?.countsByStatus && Object.keys(stats.countsByStatus).length > 0 ? (
                  Object.entries(stats.countsByStatus).map(([status, count]) => (
                    <div key={status} className="flex items-center justify-between text-xs py-1 border-b border-border-subtle">
                      <span className="font-semibold text-text">{status}</span>
                      <span className="font-mono text-text-muted">
                        <CountUp value={count} />
                      </span>
                    </div>
                  ))
                ) : (
                  <p className="text-xs text-text-muted">No tickets registered yet.</p>
                )}
              </div>
            </div>

            <div className="p-5 bg-surface border border-border rounded-xl shadow-xs">
              <h3 className="text-sm font-bold text-text mb-3">Users by Role</h3>
              <div className="space-y-2">
                {stats?.usersByRole && Object.keys(stats.usersByRole).length > 0 ? (
                  Object.entries(stats.usersByRole).map(([role, count]) => (
                    <div key={role} className="flex items-center justify-between text-xs py-1 border-b border-border-subtle">
                      <span className="font-semibold text-text">{role}</span>
                      <span className="font-mono text-text-muted">
                        <CountUp value={count} />
                      </span>
                    </div>
                  ))
                ) : (
                  <p className="text-xs text-text-muted">No users found.</p>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tabs 2, 3, 4: Ticket Lists (ALL_TICKETS, UNASSIGNED, OVERDUE) */}
      {(activeTab === 'ALL_TICKETS' || activeTab === 'UNASSIGNED' || activeTab === 'OVERDUE') && (
        <div className="space-y-4">
          {/* Controls bar */}
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3 p-3 bg-surface border border-border rounded-xl shadow-xs">
            <div className="flex items-center gap-2 flex-1">
              <input
                type="text"
                value={ticketSearch}
                onChange={(e) => setTicketSearch(e.target.value)}
                placeholder="Search ticket #, title, description, category..."
                className="w-full max-w-sm px-3 py-1.5 text-xs rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
              />

              {activeTab === 'ALL_TICKETS' && (
                <div className="hidden lg:flex items-center gap-1">
                  {(['ALL', 'OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'] as const).map((st) => (
                    <button
                      key={st}
                      type="button"
                      onClick={() => setStatusFilter(st)}
                      className={`px-2 py-1 text-xs rounded-md transition-colors ${
                        statusFilter === st
                          ? 'bg-primary text-primary-text font-semibold shadow-xs'
                          : 'bg-surface-muted text-text-muted hover:text-text'
                      }`}
                    >
                      {st}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-text-muted">Sort:</span>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as 'URGENCY' | 'NEWEST' | 'OLDEST')}
                className="px-2 py-1 text-xs rounded-md border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value="URGENCY">Urgency Level (Highest First)</option>
                <option value="NEWEST">Newest First</option>
                <option value="OLDEST">Oldest First</option>
              </select>
            </div>
          </div>

          {/* List display with staggered entrance and layout animation */}
          {isLoading ? (
            <CardSkeleton count={4} />
          ) : displayedTickets.length === 0 ? (
            <EmptyState
              title="No Tickets in View"
              description={
                ticketSearch
                  ? 'No tickets match your search query.'
                  : 'There are currently no tickets in this administrative view.'
              }
              iconType="tickets"
            />
          ) : (
            <motion.div
              layout
              initial="hidden"
              animate="show"
              variants={{
                hidden: { opacity: 0 },
                show: {
                  opacity: 1,
                  transition: { staggerChildren: 0.04 },
                },
              }}
              className="grid grid-cols-1 gap-3"
            >
              {displayedTickets.map((ticket) => (
                <motion.div
                  key={ticket.ticketId}
                  layout
                  variants={{
                    hidden: { opacity: 0, y: 10 },
                    show: { opacity: 1, y: 0, transition: { duration: 0.2 } },
                  }}
                >
                  <TicketCard
                    ticket={ticket}
                    userRole={user?.role}
                    onClick={() => handleSelectTicket(ticket)}
                    actionSlot={
                      activeTab === 'UNASSIGNED' || !ticket.assignedTo ? (
                        <div className="flex items-center gap-1.5" onClick={(e) => e.stopPropagation()}>
                          <select
                            defaultValue=""
                            onChange={(e) => {
                              if (e.target.value) {
                                handleDirectAssign(ticket.ticketId, Number(e.target.value));
                                e.target.value = '';
                              }
                            }}
                            className="px-2 py-1 text-xs rounded-md border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-primary"
                          >
                            <option value="" disabled>
                              Assign to agent...
                            </option>
                            {agents.map((ag) => (
                              <option key={ag.userId} value={ag.userId}>
                                {ag.name}
                              </option>
                            ))}
                          </select>
                        </div>
                      ) : null
                    }
                  />
                </motion.div>
              ))}
            </motion.div>
          )}
        </div>
      )}

      {/* Tab 5: Incidents */}
      {activeTab === 'INCIDENTS' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 p-4 bg-surface border border-border rounded-xl shadow-xs">
            <div>
              <h3 className="text-sm font-bold text-text">Campus Incidents</h3>
              <p className="text-xs text-text-muted mt-0.5">
                Major campus events driving ticket urgency calculation and affected student counts.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setShowCreateIncidentModal(true)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md bg-primary text-primary-text hover:bg-primary-hover transition-colors shadow-xs"
            >
              <PlusIcon className="w-3.5 h-3.5" />
              <span>Create Incident</span>
            </button>
          </div>

          {incidents.length === 0 ? (
            <div className="text-center py-16 bg-surface border border-border rounded-xl p-6">
              <p className="text-sm font-semibold text-text">No incidents recorded</p>
              <p className="text-xs text-text-muted mt-1">
                Create an incident to link related student tickets together.
              </p>
            </div>
          ) : (
            <motion.div
              layout
              initial="hidden"
              animate="show"
              variants={{
                hidden: { opacity: 0 },
                show: {
                  opacity: 1,
                  transition: { staggerChildren: 0.05 },
                },
              }}
              className="grid grid-cols-1 md:grid-cols-2 gap-3"
            >
              {incidents.map((inc) => (
                <motion.div
                  key={inc.incidentId}
                  layout
                  variants={{
                    hidden: { opacity: 0, y: 10 },
                    show: { opacity: 1, y: 0, transition: { duration: 0.2 } },
                  }}
                  className="p-4 bg-surface border border-border rounded-xl shadow-xs"
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-mono text-xs font-semibold text-text-muted">
                      INC-{inc.incidentId}
                    </span>
                    <span className="px-2 py-0.5 text-xs font-bold rounded-full bg-urgency-urgent-bg text-urgency-urgent-text border border-urgency-urgent-border">
                      {inc.severity}
                    </span>
                  </div>
                  <h4 className="text-base font-bold text-text">{inc.title}</h4>
                  <p className="text-xs text-text-muted mt-1 line-clamp-2">{inc.description}</p>
                  <div className="flex items-center justify-between text-xs text-text-muted pt-3 border-t border-border-subtle mt-3">
                    <span>Location: {inc.location}</span>
                    <span className="font-semibold text-text">
                      Affected Students: {inc.affectedStudentCount ?? 0}
                    </span>
                  </div>
                </motion.div>
              ))}
            </motion.div>
          )}
        </div>
      )}

      {/* Tab 6: Agents */}
      {activeTab === 'AGENTS' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 p-4 bg-surface border border-border rounded-xl shadow-xs">
            <div>
              <h3 className="text-sm font-bold text-text">Helpdesk Support Agents</h3>
              <p className="text-xs text-text-muted mt-0.5">
                Agents available to resolve tickets and manage campus support requests.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setShowAddAgentModal(true)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md bg-primary text-primary-text hover:bg-primary-hover transition-colors shadow-xs"
            >
              <UserPlusIcon className="w-3.5 h-3.5" />
              <span>Add Support Agent</span>
            </button>
          </div>

          <motion.div
            layout
            initial="hidden"
            animate="show"
            variants={{
              hidden: { opacity: 0 },
              show: {
                opacity: 1,
                transition: { staggerChildren: 0.05 },
              },
            }}
            className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3"
          >
            {agents.map((ag) => (
              <motion.div
                key={ag.userId}
                layout
                variants={{
                  hidden: { opacity: 0, y: 10 },
                  show: { opacity: 1, y: 0, transition: { duration: 0.2 } },
                }}
                className="p-4 bg-surface border border-border rounded-xl shadow-xs"
              >
                <div className="text-sm font-bold text-text">{ag.name}</div>
                <div className="text-xs text-text-muted mt-0.5">{ag.email}</div>
                <div className="mt-3 pt-2 border-t border-border-subtle flex items-center justify-between text-xs text-text-dim">
                  <span className="px-2 py-0.5 rounded-full bg-surface-muted text-text-muted font-medium">
                    {ag.role}
                  </span>
                  <span>Joined {new Date(ag.createdAt).toLocaleDateString()}</span>
                </div>
              </motion.div>
            ))}
          </motion.div>
        </div>
      )}

      {/* Tab 7: Closed Log */}
      {activeTab === 'CLOSED_LOG' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 p-4 bg-surface border border-border rounded-xl shadow-xs">
            <div>
              <h3 className="text-sm font-bold text-text">Closed Tickets Audit Log</h3>
              <p className="text-xs text-text-muted mt-0.5">
                Audit trail of resolved tickets, closure notes, author sign-off, and reassignment count.
              </p>
            </div>
            <div className="w-full sm:w-64">
              <input
                type="text"
                value={closedLogSearch}
                onChange={(e) => setClosedLogSearch(e.target.value)}
                placeholder="Search closed log..."
                className="w-full px-3 py-1.5 text-xs rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring"
              />
            </div>
          </div>

          {filteredClosedLog.length === 0 ? (
            <div className="text-center py-16 bg-surface border border-border rounded-xl p-6">
              <p className="text-sm font-semibold text-text">No closed tickets found</p>
              <p className="text-xs text-text-muted mt-1">
                {closedLogSearch
                  ? 'No closed tickets match your search criteria.'
                  : 'Closed tickets with closure notes will appear here once archived by an administrator.'}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-3">
              {filteredClosedLog.map((log) => (
                <div
                  key={log.ticketNumber}
                  onClick={() => handleSelectTicket({ ticketNumber: log.ticketNumber } as any)}
                  className="bento-card p-4 sm:p-5 rounded-2xl border border-border bg-surface hover:border-accent cursor-pointer transition-all duration-normal shadow-xs hover:shadow-md"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-2">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-mono text-xs font-bold text-primary">
                        {log.ticketNumber}
                      </span>
                      <span className="text-xs text-text-dim">•</span>
                      <span className="text-xs font-semibold text-text-muted">
                        {log.category}
                      </span>
                      <span className="inline-flex items-center px-2 py-0.5 text-[10px] font-bold uppercase rounded-md bg-status-closed-bg text-status-closed-text">
                        CLOSED
                      </span>
                      {log.reassignmentCount > 0 && (
                        <span className="inline-flex items-center px-2 py-0.5 text-[10px] font-semibold rounded-md bg-indigo-100 text-indigo-900 border border-indigo-200 dark:bg-indigo-950/50 dark:text-indigo-200 dark:border-indigo-700/50">
                          {log.reassignmentCount} Reassignment{log.reassignmentCount > 1 ? 's' : ''}
                        </span>
                      )}
                    </div>
                    <time className="text-xs text-text-dim">
                      Closed {new Date(log.closedAt).toLocaleString()}
                    </time>
                  </div>

                  <h4 className="text-base font-bold text-text mb-1 tracking-tight">
                    {log.title}
                  </h4>

                  <div className="text-xs text-text-muted flex items-center gap-4 flex-wrap my-2">
                    <span>
                      Agent: <strong className="text-text">{log.assignedAgentName || 'Unassigned'}</strong>
                    </span>
                    <span>•</span>
                    <span>
                      Closed by: <strong className="text-text">{log.closedBy}</strong>
                    </span>
                  </div>

                  {log.closureNote && (
                    <div className="mt-2.5 p-3 rounded-xl bg-surface-muted/60 border border-border-subtle text-xs text-text leading-relaxed">
                      <span className="font-semibold text-text-muted mr-1.5 uppercase tracking-wider text-[10px] block sm:inline">
                        Closure Note:
                      </span>
                      <span className="italic">&ldquo;{log.closureNote}&rdquo;</span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Detail Modal */}
      {selectedTicket && (
        <TicketDetailModal
          ticket={selectedTicket}
          userRole={user?.role}
          agents={agents}
          onClose={() => setSelectedTicket(null)}
          onRefresh={handleRefreshSelected}
        />
      )}

      {/* Add Agent Modal */}
      <AnimatePresence>
        {showAddAgentModal && (
          <div
            role="dialog"
            aria-modal="true"
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-overlay overflow-y-auto"
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.2 }}
              className="bg-surface border border-border rounded-xl max-w-sm w-full p-6 shadow-xl"
            >
              <h3 className="text-base font-bold text-text mb-4">Add New Support Agent</h3>
              <form onSubmit={handleAddAgent} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-text mb-1">
                    Agent Full Name
                  </label>
                  <input
                    type="text"
                    value={agentName}
                    onChange={(e) => setAgentName(e.target.value)}
                    placeholder="Bob Agent"
                    className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-text mb-1">
                    Email Address
                  </label>
                  <input
                    type="email"
                    value={agentEmail}
                    onChange={(e) => setAgentEmail(e.target.value)}
                    placeholder="agent@helpdesk.com"
                    className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-text mb-1">
                    Temporary Password
                  </label>
                  <input
                    type="password"
                    value={agentPassword}
                    onChange={(e) => setAgentPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                <div className="flex items-center justify-end gap-2 pt-3 border-t border-border-subtle">
                  <button
                    type="button"
                    onClick={() => setShowAddAgentModal(false)}
                    className="px-3 py-1.5 text-xs font-semibold rounded-md border border-border text-text-muted hover:text-text hover:bg-surface-hover transition-colors"
                  >
                    Cancel
                  </button>
                  <StatefulButton
                    type="submit"
                    disabled={isAddingAgent}
                    loadingText="Creating..."
                    successText="Created!"
                  >
                    Create Agent
                  </StatefulButton>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Create Incident Modal */}
      <AnimatePresence>
        {showCreateIncidentModal && (
          <div
            role="dialog"
            aria-modal="true"
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-overlay overflow-y-auto"
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.2 }}
              className="bg-surface border border-border rounded-xl max-w-lg w-full p-6 shadow-xl"
            >
              <h3 className="text-base font-bold text-text mb-4">Declare Campus Incident</h3>
              <form onSubmit={handleCreateIncident} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-text mb-1">
                    Incident Title
                  </label>
                  <input
                    type="text"
                    value={incidentTitle}
                    onChange={(e) => setIncidentTitle(e.target.value)}
                    placeholder="e.g. Campus Wi-Fi Outage"
                    className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-text mb-1">
                    Description
                  </label>
                  <textarea
                    rows={3}
                    value={incidentDescription}
                    onChange={(e) => setIncidentDescription(e.target.value)}
                    placeholder="Provide scope, root causes, or affected campus wings..."
                    className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-text mb-1">
                      Location
                    </label>
                    <input
                      type="text"
                      value={incidentLocation}
                      onChange={(e) => setIncidentLocation(e.target.value)}
                      placeholder="e.g. Science Block, Building 3"
                      className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-text mb-1">
                      Severity Level
                    </label>
                    <select
                      value={incidentSeverity}
                      onChange={(e) => setIncidentSeverity(e.target.value as IncidentSeverity)}
                      className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-primary"
                    >
                      <option value="LOW">LOW (+5 pts)</option>
                      <option value="MEDIUM">MEDIUM (+10 pts)</option>
                      <option value="HIGH">HIGH (+20 pts)</option>
                      <option value="CRITICAL">CRITICAL (+30 pts)</option>
                    </select>
                  </div>
                </div>

                <div className="flex items-center justify-end gap-2 pt-3 border-t border-border-subtle">
                  <button
                    type="button"
                    onClick={() => setShowCreateIncidentModal(false)}
                    className="px-3 py-1.5 text-xs font-semibold rounded-md border border-border text-text-muted hover:text-text hover:bg-surface-hover transition-colors"
                  >
                    Cancel
                  </button>
                  <StatefulButton
                    type="submit"
                    disabled={isCreatingIncident}
                    loadingText="Recording..."
                    successText="Declared!"
                  >
                    Create Incident
                  </StatefulButton>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Link Ticket to Incident Modal */}
      <AnimatePresence>
        {showLinkIncidentModal && (
          <div
            role="dialog"
            aria-modal="true"
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-overlay overflow-y-auto"
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.2 }}
              className="bg-surface border border-border rounded-xl max-w-sm w-full p-6 shadow-xl"
            >
              <h3 className="text-base font-bold text-text mb-2">Link Ticket to Incident</h3>
              <p className="text-xs text-text-muted mb-4">
                Linking a ticket recalculates ticket urgency with the incident's severity and combined affected students.
              </p>
              <form onSubmit={handleLinkTicket} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-text mb-1">
                    Incident
                  </label>
                  <select
                    value={linkIncidentId}
                    onChange={(e) => setLinkIncidentId(Number(e.target.value))}
                    className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-primary"
                    required
                  >
                    <option value="" disabled>Select Incident...</option>
                    {incidents.map((inc) => (
                      <option key={inc.incidentId} value={inc.incidentId}>
                        INC-{inc.incidentId}: {inc.title} ({inc.severity})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-text mb-1">
                    Ticket to Link
                  </label>
                  <select
                    value={linkTicketId}
                    onChange={(e) => setLinkTicketId(Number(e.target.value))}
                    className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-primary"
                    required
                  >
                    <option value="" disabled>Select Ticket...</option>
                    {allTickets.map((t) => (
                      <option key={t.ticketId} value={t.ticketId}>
                        {t.ticketNumber}: {t.title}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="flex items-center justify-end gap-2 pt-3 border-t border-border-subtle">
                  <button
                    type="button"
                    onClick={() => setShowLinkIncidentModal(false)}
                    className="px-3 py-1.5 text-xs font-semibold rounded-md border border-border text-text-muted hover:text-text hover:bg-surface-hover transition-colors"
                  >
                    Cancel
                  </button>
                  <StatefulButton
                    type="submit"
                    disabled={isLinking}
                    loadingText="Linking..."
                    successText="Linked!"
                  >
                    Link Ticket
                  </StatefulButton>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </Shell>
  );
};
