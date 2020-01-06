#!/bin/bash
make

diff <(./run.sh flow.Flow submit.MySolver flow.ConstantProp test.Test) src/test/Test.cp.out
diff <(./run.sh flow.Flow submit.MySolver flow.ConstantProp test.TestTwo) src/test/TestTwo.cp.out

diff <(./run.sh flow.Flow submit.MySolver flow.Liveness test.Test) src/test/Test.lv.out
diff <(./run.sh flow.Flow submit.MySolver flow.Liveness test.TestTwo) src/test/TestTwo.lv.out

diff <(./run.sh flow.Flow submit.MySolver submit.ReachingDefs test.Test) src/test/Test.rd.out
diff <(./run.sh flow.Flow submit.MySolver submit.ReachingDefs test.TestTwo) src/test/TestTwo.rd.out

diff <(./run.sh flow.Flow submit.MySolver submit.Faintness submit.TestFaintness) src/test/TestFaintness.out
