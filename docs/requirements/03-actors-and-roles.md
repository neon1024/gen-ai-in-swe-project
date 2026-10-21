# Actors and Roles

Roles exist at three levels. At the organization and event levels, each role includes all capabilities of the roles below it.

## System level

| Actor | Who | Capabilities |
|---|---|---|
| **Guest** | Not logged in | Browse and search public events; create an account |
| **Participant** | Any logged-in user with a confirmed email (UBB students, staff and external people) | Sign up for and cancel public activities; see own QR codes; use the inbox; give feedback; ask to join an organization; request registration of a new organization |
| **Platform Admin** | A Participant with an additional system role | Approve or reject organization requests; deactivate and reactivate user accounts and organizations; grant and revoke the Platform Admin role |

Time-based behaviour (completing events, sending reminders) is performed by the system, shown as **System (time)** in the use cases.

## Organization level

Member ⊂ Organizer ⊂ Org Admin. A user can belong to several organizations with a different role in each.

| Capability | Member | Organizer | Org Admin |
|---|:-:|:-:|:-:|
| See and sign up for the organization's private activities | ✓ | ✓ | ✓ |
| Be added to an event team | ✓ | ✓ | ✓ |
| Create events (and become their Owner) | | ✓ | ✓ |
| Edit the organization profile; handle join requests; invite members; change roles; remove members | | | ✓ |
| Owner rights on every event of the organization | | | ✓ |

An organization always has at least one Org Admin. Members can leave at any time, except the last Org Admin.

## Event level

Staff ⊂ Editor ⊂ Owner. Event team members must belong to the organizing organization. The Organizer who creates an event becomes its Owner.

| Capability | Staff | Editor | Owner |
|---|:-:|:-:|:-:|
| View signups and waitlist; check in participants; see and complete own tasks; view feedback and report | ✓ | ✓ | ✓ |
| Edit event, activities and agenda; publish; invite people to private activities; create and assign tasks; send announcements; add and remove Staff | | ✓ | ✓ |
| Promote to and remove from Editor; transfer ownership; cancel the event or an activity; delete a draft | | | ✓ |
