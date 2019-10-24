# Artemis produced

State mismatch after the (1 -> 2) epoch transition.

ZRNT has known issues wrt Crosslinks in ths below diff.

Artemis passing state tests but having different balances in this transition
points to a lack of coverage in the spec tests.

## Know errors in clients

### ZRNT

Crosslink and Balances mismatch

Diff with pyspec (A) and zrnt (B):
```
BeaconState objects A and B are different:
modified bytes: .CrosslinksState.CurrentCrosslinks[0].ParentRoot
from = f4905dbaa6c8981445e359f32bbe528f840a587a5be7944fa8b267abcb8e9fb8
  to = c78009fdf07fc56a11f122370658a353aaa542ed63e44c4bc15ff4cd105ab33c
modified bytes: .CrosslinksState.CurrentCrosslinks[1].ParentRoot
from = 3a5408fb8d9652046dbdd67bd50d9567f41408673f5ee5488e710b4ca44d8ed2
  to = c78009fdf07fc56a11f122370658a353aaa542ed63e44c4bc15ff4cd105ab33c
modified bytes: .CrosslinksState.CurrentCrosslinks[2].ParentRoot
from = 1ca52383f23fb4bfd7b65431c0c08006718144e0a86a9c04d939aa0b28c80334
  to = c78009fdf07fc56a11f122370658a353aaa542ed63e44c4bc15ff4cd105ab33c
modified bytes: .CrosslinksState.CurrentCrosslinks[3].ParentRoot
from = 1cdb545c80fb6780cd2d366512e593a1967600810bbd27dfae78eb45340f76cf
  to = c78009fdf07fc56a11f122370658a353aaa542ed63e44c4bc15ff4cd105ab33c
modified bytes: .CrosslinksState.CurrentCrosslinks[4].ParentRoot
from = a67977176bd731bd1b2ef8e7ccd842c26714848d186f9762140e676177ce37d7
  to = c78009fdf07fc56a11f122370658a353aaa542ed63e44c4bc15ff4cd105ab33c
modified bytes: .CrosslinksState.CurrentCrosslinks[6].ParentRoot
from = c78009fdf07fc56a11f122370658a353aaa542ed63e44c4bc15ff4cd105ab33c
  to = 0000000000000000000000000000000000000000000000000000000000000000
modified bytes: .CrosslinksState.CurrentCrosslinks[7].ParentRoot
from = c78009fdf07fc56a11f122370658a353aaa542ed63e44c4bc15ff4cd105ab33c
  to = 0000000000000000000000000000000000000000000000000000000000000000
modified: .CrosslinksState.CurrentCrosslinks[0].EndEpoch, from = 0x1; to = 0x0
modified: .CrosslinksState.CurrentCrosslinks[1].EndEpoch, from = 0x1; to = 0x0
modified: .CrosslinksState.CurrentCrosslinks[2].EndEpoch, from = 0x1; to = 0x0
modified: .CrosslinksState.CurrentCrosslinks[3].EndEpoch, from = 0x1; to = 0x0
modified: .CrosslinksState.CurrentCrosslinks[4].EndEpoch, from = 0x1; to = 0x0
modified: .CrosslinksState.CurrentCrosslinks[6].Shard, from = 0x6; to = 0x0
modified: .CrosslinksState.CurrentCrosslinks[7].Shard, from = 0x7; to = 0x0
modified: .RegistryState.BalancesState.Balances[0], from = 0x773850f4a; to = 0x773739726
modified: .RegistryState.BalancesState.Balances[10], from = 0x773716822; to = 0x7737e823d
modified: .RegistryState.BalancesState.Balances[11], from = 0x773850f4a; to = 0x77380b141
modified: .RegistryState.BalancesState.Balances[12], from = 0x773850f4a; to = 0x77380b141
modified: .RegistryState.BalancesState.Balances[13], from = 0x773739726; to = 0x77380b141
modified: .RegistryState.BalancesState.Balances[15], from = 0x773716822; to = 0x7737e823d
modified: .RegistryState.BalancesState.Balances[1], from = 0x773850f4a; to = 0x773739726
modified: .RegistryState.BalancesState.Balances[5], from = 0x773739726; to = 0x77380b141
modified: .RegistryState.BalancesState.Balances[7], from = 0x773739726; to = 0x77380b141
modified: .RegistryState.BalancesState.Balances[8], from = 0x77382e046; to = 0x7737e823d
modified: .RegistryState.BalancesState.Balances[9], from = 0x773850f4a; to = 0x773739726

```

### Artemis

Balances mismatch

Diff with pyspec (A) and Artemis (B):
```
BeaconState objects A and B are different:
modified: .RegistryState.BalancesState.Balances[0], from = 0x773850f4a; to = 0x7737c5338
modified: .RegistryState.BalancesState.Balances[10], from = 0x773716822; to = 0x7737a2434
modified: .RegistryState.BalancesState.Balances[11], from = 0x773850f4a; to = 0x7737c5338
modified: .RegistryState.BalancesState.Balances[12], from = 0x773850f4a; to = 0x7737c5338
modified: .RegistryState.BalancesState.Balances[13], from = 0x773739726; to = 0x7737c5338
modified: .RegistryState.BalancesState.Balances[14], from = 0x773716822; to = 0x7737a2434
modified: .RegistryState.BalancesState.Balances[15], from = 0x773716822; to = 0x7737a2434
modified: .RegistryState.BalancesState.Balances[1], from = 0x773850f4a; to = 0x7737c5338
modified: .RegistryState.BalancesState.Balances[2], from = 0x773716822; to = 0x7737a2434
modified: .RegistryState.BalancesState.Balances[3], from = 0x773716822; to = 0x7737a2434
modified: .RegistryState.BalancesState.Balances[4], from = 0x773716822; to = 0x7737a2434
modified: .RegistryState.BalancesState.Balances[5], from = 0x773739726; to = 0x7737c5338
modified: .RegistryState.BalancesState.Balances[6], from = 0x773716822; to = 0x7737a2434
modified: .RegistryState.BalancesState.Balances[7], from = 0x773739726; to = 0x7737c5338
modified: .RegistryState.BalancesState.Balances[8], from = 0x77382e046; to = 0x7737a2434
modified: .RegistryState.BalancesState.Balances[9], from = 0x773850f4a; to = 0x7737c5338
```