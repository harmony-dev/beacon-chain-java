package org.ethereum.beacon.emulator.config.version;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigV1 implements Config {
  private final int version = 1;
  private ConfigPart config;
  private PlanPart plan;

  public ConfigPart getConfig() {
    return config;
  }

  public void setConfig(ConfigPart config) {
    this.config = config;
  }

  public PlanPart getPlan() {
    return plan;
  }

  public void setPlan(PlanPart plan) {
    this.plan = plan;
  }

  @Override
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    assert version == this.version;
  }

  public static class PlanPart {
    private List<ActionPlanPart> sync;
    private List<ActionPlanPart> validator;

    public List<ActionPlanPart> getSync() {
      return sync;
    }

    public void setSync(List<ActionPlanPart> sync) {
      this.sync = sync;
    }

    public List<ActionPlanPart> getValidator() {
      return validator;
    }

    public void setValidator(List<ActionPlanPart> validator) {
      this.validator = validator;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
    @JsonSubTypes({
      @JsonSubTypes.Type(value = ActionRun.class, name = "run"),
      @JsonSubTypes.Type(value = ActionEmulate.class, name = "emulate"),
      @JsonSubTypes.Type(value = ActionDeposit.class, name = "deposit")
    })
    public abstract static class ActionPlanPart {}

    public static class ActionRun extends ActionPlanPart {
      public ActionRun() {}

      public ActionRun(String s) {}
    }

    public static class ActionEmulate extends ActionPlanPart {
      private int count;

      public int getCount() {
        return count;
      }

      public void setCount(int count) {
        this.count = count;
      }
    }

    public static class ActionDeposit extends ActionPlanPart {
      private String creator;
      private String sender;
      private Long gasLimit;
      private String eth1From;
      private String eth1PrivKey;
      private String withdrawalCredentials;
      private Long amount;

      public String getCreator() {
        return creator;
      }

      public void setCreator(String creator) {
        this.creator = creator;
      }

      public String getSender() {
        return sender;
      }

      public void setSender(String sender) {
        this.sender = sender;
      }

      public Long getGasLimit() {
        return gasLimit;
      }

      public void setGasLimit(Long gasLimit) {
        this.gasLimit = gasLimit;
      }

      public String getEth1From() {
        return eth1From;
      }

      public void setEth1From(String eth1From) {
        this.eth1From = eth1From;
      }

      public String getEth1PrivKey() {
        return eth1PrivKey;
      }

      public void setEth1PrivKey(String eth1PrivKey) {
        this.eth1PrivKey = eth1PrivKey;
      }

      public String getWithdrawalCredentials() {
        return withdrawalCredentials;
      }

      public void setWithdrawalCredentials(String withdrawalCredentials) {
        this.withdrawalCredentials = withdrawalCredentials;
      }

      public Long getAmount() {
        return amount;
      }

      public void setAmount(Long amount) {
        this.amount = amount;
      }
    }
  }

  public class ConfigPart {
    private String chainSpec;
    private String db;
    private ValidatorPart validator;

    public String getChainSpec() {
      return chainSpec;
    }

    public void setChainSpec(String chainSpec) {
      this.chainSpec = chainSpec;
    }

    public String getDb() {
      return db;
    }

    public void setDb(String db) {
      this.db = db;
    }

    public ValidatorPart getValidator() {
      return validator;
    }

    public void setValidator(ValidatorPart validator) {
      this.validator = validator;
    }

    public class ValidatorPart {
      private Map<String, String> contract;
      private SignerPart signer;

      public Map<String, String> getContract() {
        return contract;
      }

      public void setContract(Map<String, String> contract) {
        this.contract = contract;
      }

      public SignerPart getSigner() {
        return signer;
      }

      public void setSigner(SignerPart signer) {
        this.signer = signer;
      }

      public class SignerPart {
        private ImplementationPart implementation;

        public ImplementationPart getImplementation() {
          return implementation;
        }

        public void setImplementation(ImplementationPart implementation) {
          this.implementation = implementation;
        }

        public class ImplementationPart {
          private String clazz;
          private Map<String, String> input;

          public String getClazz() {
            return clazz;
          }

          public void setClazz(String clazz) {
            this.clazz = clazz;
          }

          public Map<String, String> getInput() {
            return input;
          }

          public void setInput(Map<String, String> input) {
            this.input = input;
          }
        }
      }
    }
  }
}
