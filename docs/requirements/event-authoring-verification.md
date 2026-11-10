# Event Authoring acceptance-to-test matrix

Status: **Approved slice and executable plan; EA-01 infrastructure verified; EA-02 draft backend verified under fixtures; EA-03 activity/agenda backend verified under fixtures; UI and real integration pending**. Scope: all 17 acceptance criteria of #17–#21 / US-14–18 in [the approved stories](08-user-stories.md). The [integration contract](../integration/event-authoring-contract.md) defines approved for this isolated slice components, external dependencies and approval questions. Test identifiers below are planned Java/TypeScript-safe names, not claims that files/tests exist. Actual feature files, commands and results will be added as each TDD task and its reviews finish. EA-01 migration/constraint and shell checks do not complete a story criterion. Human Review Status: Pending student review.

#17 has backend-only fixture evidence below; UI and real integration remain pending. #18/#19 have backend-only fixture evidence below; #20 has backend-only fixture evidence below; #21 has backend-only fixture evidence below (reviewed at 4cce076). No full-story completion is claimed. A fixture is not production identity, membership, search or FIFO integration. The labels distinguish the intended level; they do not claim passing tests.

| Issue | Story | FR | BR | Acceptance criterion | Planned test identifier | Proposed component / test type | Integration dependency |
|---|---|---|---|---|---|---|---|
| [#17](https://github.com/neon1024/gen-ai-in-swe-project/issues/17) | US-14 | FR-EVT-01 | BR-03 | Required event fields and eligible organization | `US14_createDraft_requiresFieldsAndOrganizerRole` | Authoring domain/service/API/UI as applicable; Domain/service + MVC + UI | Real identity/membership adapter pending |
| [#17](https://github.com/neon1024/gen-ai-in-swe-project/issues/17) | US-14 | FR-EVT-01 | BR-03 | Creator becomes actual Owner | `US14_createDraft_assignsTrustedCreatorAsOwner` | Authoring domain/service/API/UI as applicable; Service + PostgreSQL | Session/eligibility adapter pending; no client actor trusted |
| [#17](https://github.com/neon1024/gen-ai-in-swe-project/issues/17) | US-14 | FR-EVT-01 | BR-03 | Draft visible only to team and same-org Org Admin | `BR03_readDraft_checksTeamAndOrganizationContext` | Authoring domain/service/API/UI as applicable; Authorization + MVC | Real org/team adapters pending; Staff read and cross-org denial fixtures |
| [#18](https://github.com/neon1024/gen-ai-in-swe-project/issues/18) | US-15 | FR-EVT-02 | BR-07/08 | Activity fields, relationships, optional windows and public Mid-term visibility | `US15_activityWrite_preservesFieldsAndParent` | Authoring domain/service/API/UI as applicable; Service + PostgreSQL + UI | Complete-write interpretation and feature setup approved for this isolated slice |
| [#18](https://github.com/neon1024/gen-ai-in-swe-project/issues/18) | US-15 | FR-EVT-02 | BR-07 | Reject invalid interval and capacity below one | `BR07_activityWrite_rejectsNonPositiveIntervalOrCapacity` | Authoring domain/service/API/UI as applicable; Domain + MVC + SQL constraints | Strict end > start is approved; setup pending |
| [#18](https://github.com/neon1024/gen-ai-in-swe-project/issues/18) | US-15 | FR-EVT-02 | BR-08 | Default windows open at publication and close at activity start | `BR08_effectiveRegistrationWindow_usesPublicationAndStart` | Authoring domain/service/API/UI as applicable; Domain + API contract | Temporal interpretation for added Published activity approved by the requesting student |
| [#18](https://github.com/neon1024/gen-ai-in-swe-project/issues/18) | US-15 | FR-EVT-02 | BR-08 | Refuse explicit close after activity start | `BR08_activityWrite_rejectsClosingAfterStart` | Authoring domain/service/API/UI as applicable; Domain + MVC + UI | Plan approved; feature verification pending |
| [#19](https://github.com/neon1024/gen-ai-in-swe-project/issues/19) | US-16 | FR-EVT-03 | BR-07 | Agenda time/title and optional speaker | `US16_agendaCrud_preservesTimeTitleAndOptionalSpeaker` | Authoring domain/service/API/UI as applicable; Service + PostgreSQL + UI | Real authorization adapter pending |
| [#19](https://github.com/neon1024/gen-ai-in-swe-project/issues/19) | US-16 | FR-EVT-03 | BR-07 | Reject agenda outside activity interval | `BR07_agendaWrite_andScheduleChange_preserveInterval` | Authoring domain/service/API/UI as applicable; Domain + service + MVC | Inclusive endpoints and atomic schedule-edit refusal approved for this isolated slice |
| [#20](https://github.com/neon1024/gen-ai-in-swe-project/issues/20) | US-17 | FR-EVT-04 | BR-01/02 | At least one activity with required interval/place/capacity | `BR02_publish_validatesEveryActivity` | Authoring domain/service/API/UI as applicable; Domain/service + PostgreSQL | Real authorization adapter pending |
| [#20](https://github.com/neon1024/gen-ai-in-swe-project/issues/20) | US-17 | FR-EVT-04 | BR-02 | Refuse publication with complete list of problems | `US17_publish_returnsAllReadinessViolations` | Authoring domain/service/API/UI as applicable; Service + MVC + UI | Error contract approved for isolated authoring; feature verification pending |
| [#20](https://github.com/neon1024/gen-ai-in-swe-project/issues/20) | US-17 | FR-EVT-04 | BR-01/08 | Published event appears in search and follows windows | `US17_committedPublication_isConsumedByDiscoveryAndRegistration` | Authoring domain/service/API/UI as applicable; Real cross-module integration/E2E | Authoring is not connected to PR #58 discovery or real registration; local projection/doubles cannot complete criterion |
| [#20](https://github.com/neon1024/gen-ai-in-swe-project/issues/20) | US-17 | FR-EVT-04 | BR-01 | Staff cannot publish | `US17_publish_deniesStaffButAllowsEditorOwnerAndOrgAdmin` | Authoring domain/service/API/UI as applicable; Authorization + MVC | Real org/team adapters pending; no extra platform-admin privilege |
| [#21](https://github.com/neon1024/gen-ai-in-swe-project/issues/21) | US-18 | FR-EVT-05 | BR-06/12/14 | Edit Published event/activity/agenda and add activities | `US18_editPublished_preservesSignupsAndNestedIntegrity` | Authoring domain/service/API/UI as applicable; Service + PostgreSQL + UI | Authoring portion independently testable after approval; real signup preservation pending |
| [#21](https://github.com/neon1024/gen-ai-in-swe-project/issues/21) | US-18 | FR-EVT-05 | BR-14 | Capacity never below confirmed signups | `BR14_capacityChange_enforcesAuthoritativeConfirmedCount` | Authoring domain/service/API/UI as applicable; Transactional integration/concurrency | Same-transaction signup adapter and shared lock protocol absent |
| [#21](https://github.com/neon1024/gen-ai-in-swe-project/issues/21) | US-18 | FR-EVT-05 | BR-12 | Capacity increase promotes eligible waitlisted people | `BR12_capacityIncrease_invokesAtomicRegistrationPromotion` | Authoring domain/service/API/UI as applicable; Contract double + real cross-module integration | Actual FIFO owned by #33; doubles verify invocation/rollback only, not real promotion |
| [#21](https://github.com/neon1024/gen-ai-in-swe-project/issues/21) | US-18 | FR-EVT-05 | BR-14 | Activity with signups cannot be deleted | `BR14_activityRemoval_refusesAnySignupHistory` | Authoring domain/service/API/UI as applicable; Service + transactional integration + UI | Actual signup guard absent; historical-signup interpretation approved for this isolated slice |

## Cross-cutting verification cases

Plan tests for untrusted client identity/Owner/state fields; absent/failed external adapters; nested resource ID substitution; same-org Org Admin versus unrelated Platform Admin; Completed/Cancelled read-only; repeated/concurrent publish; stale version conflicts; transaction rollback on failed signup-sensitive mutation; agenda stranding after interval edits; registration-window defaults after schedule edits; public-only Mid-term writes; and real UI loading/error/success/empty/accessibility states. Test publication and expiry using an injected clock, not the host timezone.

## Evidence updates after approval

For every implementation task record the expected RED command/result, GREEN result, regression command/result, actual file/test identifiers, implementer and independent spec/quality reviews, real adapter versus test-double use, and last execution timestamp. Mark a criterion verified with doubles separately from verified against real services. Production-readiness and full US-17/US-18 completion remain pending real integrations where indicated. Never auto-close issues based on generated code or unit-test success.

## EA-02 backend evidence — #17 (2026-11-10)

At 7263e0c, independent spec/quality re-reviews passed and coordinator Maven test passed 95 tests, 0 failures/errors/skips. Source tests are under backend/src/test/java/com/ubb/genai/events/authoring/. These exercise real domain/service, HTTP and PostgreSQL; identity, eligibility, organization membership and team roles are explicit fixtures, not real services.

| Criterion | Actual backend checks | Remaining acceptance |
|---|---|---|
| US14 required metadata and eligible org | DraftAuthoringTests.US14_eligibleOrganizerOrAdminCreates, validatesAllRequiredFields, US14_staffOrNonOrganizerCannotCreate; DraftAuthoringApiTests.invalidCategoryAndRequiredFieldsReturnSafeEnvelope and malformed/NUL parameterized tests | UI and real identity/organization integration |
| US14 actual creator/current Owner | DraftAuthoringTests.US14_eligibleOrganizerOrAdminCreates, formerOwnerHasNoPermanentRights; AuthoringPersistenceTests.committedDraftRoundTripsAndFailedTransactionLeavesNoRow | UI and real authoritative identity/membership integration |
| BR03 restricted draft reads | DraftAuthoringTests.actualTeamReads, ordinaryOrganizerAndCrossOrgConcealed, ownerStillNeedsCurrentMembershipAndEligibility; DraftAuthoringApiTests keyed differential-access regression and redaction checks | Real identity/org/team integration; UI |

Additional evidence: AuthoringAdapterConfigurationTests checks ordinary adapter/service/Clock replacement; AuthoringWiringTests checks CSRF/default identity and generated OpenAPI201+Location. Student implementation review remains pending.

## EA-03 backend evidence — #18/#19 (2026-11-10)

Independent specification/quality re-reviews approved 4ecc06f; coordinator 123 backend tests passed with 0 failures/errors/skips. Actual tests under backend/src/test/java/com/ubb/genai/events/authoring/ use real PostgreSQL and explicit upstream fixtures. No complete-story or real-registration claim is made.

| Criterion | Actual backend checks | Remaining acceptance |
|---|---|---|
| US15 complete fields/offsets/parent | ActivityAgendaApiTests.US15_completeWriteRoundTripsOffsetsAndVersion | UI and real access integration |
| BR07 strict interval/capacity | ActivityScheduleTests.BR07_endMustBeStrictlyLaterByInstant; ActivityAgendaApiTests.US15_invalidActivityWritesRollBack | UI; real access integration |
| BR08 default windows | ActivityScheduleTests.BR08_defaultsFollowPublicationCreationAndStart; AgendaAuthoringTests.BR08_defaultCloseFollowsScheduleButExplicitWindowAndAgendaStayUnchanged | UI and real registration consumer |
| BR08 explicit close | ActivityScheduleTests.BR08_explicitWindowMustBeOrderedAndCloseByStart; ActivityAgendaApiTests.US16_scheduleReplacementCannotStrandAgendaOrExplicitClose | UI |
| US16 agenda fields and speaker | ActivityAgendaApiTests.US16_agendaCrudIncludesEndpointsOffsetsOptionalSpeakerAndSortedReads | UI and real access integration |
| BR07 agenda interval | ActivityAgendaApiTests.US16_invalidAgendaInputNeverWritesOrBumpsVersion and US16_scheduleReplacementCannotStrandAgendaOrExplicitClose | UI |

Cross-cutting: ActivityAgendaApiTests.foreignNestedResourcesStayConcealedBeforeStaleVersionOrStaffDenial covers ordering/concealment. AgendaAuthoringTests verifies competing writes and guard rollback. AggregateReadConsistencyTests verifies coherent root/activity/agenda snapshots across committed concurrent writes. Guard fixtures do not prove actual signup preservation/FIFO. Student review remains pending.

## Publication #20 backend evidence — current stopping point (2026-11-10)

Independent Terra specification/quality reviews approved db799b9. Coordinator Maven verify passed 140 tests, 0 failures/errors/skips. Identity/org/team remain explicit fixtures; publication, versions, transactions and projection use real PostgreSQL. No full US-17 delivery is claimed.

| Criterion | Actual checks | Status / remaining acceptance |
|---|---|---|
| Activity readiness | PublicationTests.US17_revalidatesEveryActivityAndAccumulatesAllProblems; PublicationApiTests.US17_emptyReadinessUses422Envelope | Backend verified; UI pending |
| Complete problems list | Same readiness tests include separate opening/closing errors and Public-only visibility | Backend verified; UI pending |
| Search visibility and windows | PublicationApiTests.US17_publicationPersistsTimestampWindowsAndVersion; PublicationTests.US17_latePublicationKeepsAlreadyEndedRegistrationWindowClosed; AuthoringConcurrencyTests.US17_projectionOnlyShowsCommittedPublishedAggregates; AggregateReadConsistencyTests.publishedProjectionKeepsRootAndChildrenInOneCommittedSnapshot | Local committed projection/windows verified; actual discovery and registration consumers pending |
| Staff cannot publish | PublicationTests.US17_staffDeniedBeforeVersionAndReadiness; PublicationApiTests.US17_sessionCsrfAndStaffAuthorizationRemainDistinct | Backend fixture verified; real access integration/UI pending |

AuthoringConcurrencyTests.US17_concurrentPublishesSerializeAndOnlyOneWins verifies concurrent publication. The temporary gate test was replaced when #21 resumed (see the #21 section below). Human Review Status: Pending student review.

## Issue #21 backend evidence (2026-11-10)

Independent specification review SPEC COMPLIANT and quality review APPROVED; coordinator Maven verify at 4cce076 passed 170 tests, 0 failures/errors/skips. Real PostgreSQL; identity/org/team/registration are explicit fixtures or call probes. No full US-18 delivery is claimed.

| Criterion | Actual checks | Remaining acceptance |
|---|---|---|
| Edit Published event/activity/agenda; add activities | PublishedEditingTests.US18_detailsEditPreservesImmutableMetadataAndActivities, US18_publishedActivitiesAndAgendaRemainEditableWithPreservedAgenda, US18_editorAndSameOrganizationAdminCanEditMetadata; PublicationApiTests.US18_eventDetailsEndpointPersistsOnlyAllowedMetadata, US18_publishedMutationsAreAllowedForOwnerEditorAndSameOrganizationAdmin, US18_publishedMutationsKeepStaffForbiddenAndForeignNestedResourcesHidden, US18_publishedActivityEditsRemainPublicOnly | UI; real access integration |
| Capacity never below confirmed signups | RegistrationAuthoringContractTests.BR12_capacityCannotFallBelowAuthoritativeConfirmedCount, BR12_staleAndConcurrentCapacityWritesCannotBypassRootVersion, BR12_guardOrPromotionFailureRollsBackAuthoringRowsAndVersion; PublicationApiTests.US18_missingProductionRegistrationAdapterRefusesCapacityChange | Real registration adapter and agreed lock protocol |
| Raising capacity promotes from the waitlist | RegistrationAuthoringContractTests.BR12_everyPublishedCapacityChangeIsGuardedAndIncreasePromotesInTransaction, BR12_neverPromotesAtOrAfterUpdatedStart, BR12_clockIsEvaluatedAfterGuardLocksAndUsesUpdatedStart | Actual FIFO (#33) and real promotion |
| Activity with signups cannot be deleted | RegistrationAuthoringContractTests.BR14_anySignupHistoryBlocksRemovalWithZeroConfirmed; PublishedEditingTests.US18_lastPublishedActivityCannotBeRemovedEvenWithoutSignups | Real signup guard; cancellation is owned by #23 |

Draft capacity regression: RegistrationAuthoringContractTests.EA03_draftCapacityChangesNeverInvokeRegistrationGuard and PublicationApiTests.EA03_draftCapacityChangeSucceedsWithoutRegistrationAdapter. Human Review Status: Pending student review.
