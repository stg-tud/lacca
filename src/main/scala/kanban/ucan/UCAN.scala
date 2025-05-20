package kanban.ucan

import java.security.*
import java.time.Instant
import java.util.Base64
import scala.util.{Failure, Success, Try}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, HCursor, JsonObject, DecodingFailure}
import io.circe.generic.semiauto._
import io.circe.parser.decode

object Ucan {
    val Version = "0.10.0"
    private val base64urlEncoder = Base64.getUrlEncoder.withoutPadding
    private val base64urlDecoder = Base64.getUrlDecoder
    private val cidParser = createDefaultCidParser()
    private val didParser = createDefaultDidParser()

    // Use the CID class from CIDParser
    type CID = CidParser#CID
    case class Capability(
        resource: String, // Resource URI
        abilities: Map[String, List[Json]] // Map of ability -> list of caveats
    )

    case class Header(alg: String, typ: String)
    case class Payload(
        ucv: String,
        iss: String,
        aud: String,
        nbf: Option[Instant] = None,
        exp: Option[Instant],
        nnc: Option[String] = None,
        fct: Option[Map[String, Json]] = None,
        cap: List[Capability],
        prf: Option[List[CID]] = None
    )
    case class Ucan(
        header: Header,
        payload: Payload,
        signedData: String, // List[Byte] as base64
        signature: String // List[Byte] as base64
    )

    case class RevocationMessage(
        iss: String, // DID of the revoker
        revoke: String, // canonical CID of the UCAN being revoked
        challenge: String // Base64 signature of "REVOKE:${canonicalUcanCid}"
    )

    object RevocationMessage {
        implicit val revocationMessageEncoder: Encoder[RevocationMessage] = deriveEncoder
        implicit val revocationMessageDecoder: Decoder[RevocationMessage] = deriveDecoder
    }

    trait RevocationStore {
        def isRevoked(cid: CID): Boolean
        def addRevocation(revocation: RevocationMessage): Try[Unit]
    }

    class InMemoryRevocationStore extends RevocationStore {
        private val revocations = scala.collection.mutable.Map[String, RevocationMessage]()

        def isRevoked(cid: CID): Boolean = {
            revocations.contains(cid.encode())
        }

        def addRevocation(revocation: RevocationMessage): Try[Unit] = Try {
            revocations.put(revocation.revoke, revocation)
            ()
        }
    }

    // Proof resolution interface
    trait ProofResolver {
        def resolve(cid: CID): Try[Ucan]
        def proofs(): Map[String, String]
        def revocationStore: RevocationStore
    }

    class InMemoryProofResolver(
        private var _proofs: Map[String, String] = Map.empty,
        val revocationStore: RevocationStore = new InMemoryRevocationStore()
    ) extends ProofResolver {
        def resolve(cid: CID): Try[Ucan] = {
            val cidStr = cid.encode()

            // check if the UCAN is revoked
            if (revocationStore.isRevoked(cid)) {
                return Failure(new SecurityException(s"UCAN has been revoked: cid=$cidStr"))
            }

            proofs().get(cidStr) match {
                case Some(token) => decodeJwt(token)
                case None        => Failure(new SecurityException(s"Proof not found: cid=$cidStr"))
            }
        }

        def proofs(): Map[String, String] = this._proofs

        def addProof(cid: String, token: String): Unit = {
            this._proofs = this._proofs + (cid -> token)
        }
    }

    def createCID(ucan: Ucan): CID = {
        val tokenString = encodeJwt(ucan)
        cidParser.create(
          1,
          cidParser.CodecRaw,
          cidParser.Sha2_256,
          tokenString.getBytes("UTF-8")
        )
    }

    implicit val capabilityEncoder: Encoder[Capability] =
        new Encoder[Capability] {
            final def apply(c: Capability): Json = {
                Json.obj(
                  c.resource -> c.abilities.map { case (ability, caveats) =>
                      ability -> Json.arr(caveats: _*)
                  }.asJson
                )
            }
        }

    implicit val cidEncoder: Encoder[CID] = new Encoder[CID] {
        final def apply(cid: CID): Json = {
            Json.fromString(cid.encode())
        }
    }
    implicit val headerEncoder: Encoder[Header] = deriveEncoder
    implicit val payloadEncoder: Encoder[Payload] = Encoder.forProduct9(
      "ucv", // Version
      "iss", // Issuer DID
      "aud", // Audience DID
      "nbf", // Valid from (Not Before)
      "exp", // Expiration (Valid Until)
      "nnc", // Nonce (Optional String)
      "fct", // Facts (Optional List of Json)
      "cap", // Capabilities (List of Capability)
      "prf" // Proofs (Optional List of CID)
    )(payload =>
        (
          payload.ucv, // "ucv" -> Version
          payload.iss, // "iss" -> Issuer DID
          payload.aud, // "aud" -> Audience DID
          payload.nbf
              .map(_.getEpochSecond)
              .asJson, // "nbf" -> validFrom (Optional, in seconds)
          payload.exp
              .map(_.getEpochSecond.asJson)
              .getOrElse(Json.Null), // "exp" -> Expiration (Optional, in seconds)
          payload.nnc.asJson, // "nnc" -> Nonce (Optional String)
          payload.fct.asJson, // "fct" -> Facts (Optional List of Json)
          payload.cap.asJson, // "cap" -> Capabilities (List of Capability)
          payload.prf.asJson // "prf" -> Proofs (Optional List of CID)
        )
    )
    implicit val ucanEncoder: Encoder[Ucan] = deriveEncoder

    implicit val capabilityDecoder: Decoder[Capability] =
        new Decoder[Capability] {
            final def apply(c: HCursor): Decoder.Result[Capability] = {
                for {
                    resource <- c.keys
                        .flatMap(_.headOption)
                        .toRight(DecodingFailure("Missing resource", c.history))
                    resourceCursor = c.downField(resource)

                    abilitiesMap <- resourceCursor.as[Map[String, List[Json]]]
                } yield Capability(resource, abilitiesMap)
            }
        }

    implicit val cidDecoder: Decoder[CID] = new Decoder[CID] {
        final def apply(c: HCursor): Decoder.Result[CID] = {
            for {
                cidStr <- c.as[String]
                cid <- cidParser
                    .parse(cidStr)
                    .toEither
                    .left
                    .map(e => DecodingFailure(e.getMessage, c.history))
            } yield cid
        }
    }
    implicit val headerDecoder: Decoder[Header] = deriveDecoder
    implicit val payloadDecoder: Decoder[Payload] = (cursor: HCursor) =>
        for {
            ucv <- cursor.get[String]("ucv")
            iss <- cursor.get[String]("iss")
            aud <- cursor.get[String]("aud")
            nbf <- cursor.get[Option[Long]]("nbf").map(_.map(Instant.ofEpochSecond))
            exp <- cursor.get[Option[Long]]("exp").map(_.map(Instant.ofEpochSecond))
            nnc <- cursor.get[Option[String]]("nnc")
            fct <- cursor.get[Option[Map[String, Json]]]("fct")
            cap <- cursor.get[List[Capability]]("cap")
            prf <- cursor.get[Option[List[CID]]]("prf")
        } yield Payload(ucv, iss, aud, nbf, exp, nnc, fct, cap, prf)
    implicit val ucanDecoder: Decoder[Ucan] = deriveDecoder

    class PayloadBuilder {
        private var issuer: String = ""
        private var audience: String = ""
        private var capabilities: List[Capability] = Nil
        private var proofs: List[CID] = Nil
        private var expiration: Option[Instant] = None
        private var notBefore: Option[Instant] = None
        private var facts: Map[String, Json] = Map.empty
        private var nonce: Option[String] = None

        def issuedBy(issuer: KeyMaterial): PayloadBuilder = {
            this.issuer = didParser.keyMaterialToDid(issuer)
            this
        }

        def forAudience(audience: KeyMaterial): PayloadBuilder = {
            this.audience = didParser.keyMaterialToDid(audience)
            this
        }

        def withLifetime(seconds: Long): PayloadBuilder = {
            this.notBefore = Some(Instant.now())
            this.expiration = Some(Instant.now().plusSeconds(seconds))
            this
        }

        def claimingCapability(
            resource: String,
            ability: String,
            caveats: List[Json] = List(Json.obj())
        ): PayloadBuilder = {
            val existingCapability = this.capabilities.find(_.resource == resource)

            val updatedCapabilities = existingCapability match {
                case Some(cap) =>
                    // Update existing capability with new ability and caveats
                    val updatedAbilities = cap.abilities + (ability -> caveats)
                    val updatedCap = cap.copy(abilities = updatedAbilities)
                    this.capabilities.filterNot(_.resource == resource) :+ updatedCap
                case None =>
                    // Create new capability
                    val newCap = Capability(resource, Map(ability -> caveats))
                    this.capabilities :+ newCap
            }

            this.capabilities = updatedCapabilities
            this
        }

        def witnessedBy(proof: CID): PayloadBuilder = {
            this.proofs = proof :: this.proofs
            this
        }

        def withFact(category: String, value: Json): PayloadBuilder = {
            this.facts = this.facts + (category -> value)
            this
        }

        def withNonce(): PayloadBuilder = {
            val randomBytes = new Array[Byte](16)
            SecureRandom.getInstanceStrong.nextBytes(randomBytes)
            this.nonce = Some(
              Base64.getUrlEncoder.withoutPadding.encodeToString(randomBytes)
            )
            this
        }

        def build(): Payload = {
            require(issuer.nonEmpty, "Issuer must be set")
            require(audience.nonEmpty, "Audience must be set")
            require(capabilities.nonEmpty, "At least one capability required")
            // require(expiration, "Expiratin must be set")
            Payload(
              ucv = Version,
              iss = issuer,
              aud = audience,
              exp = expiration,
              nbf = notBefore,
              nnc = nonce,
              cap = capabilities,
              prf = if (proofs.isEmpty) None else Some(proofs),
              fct = if (facts.isEmpty) None else Some(facts)
            )
        }
    }

    def builder(): PayloadBuilder = new PayloadBuilder

    def sign(payload: Payload, keyMaterial: KeyMaterial): Ucan = {
        val header = Header(keyMaterial.jwtAlg, "JWT")
        val encodedHeader = base64urlEncoder.encodeToString(
          Json
              .obj("alg" -> header.alg.asJson, "typ" -> header.typ.asJson)
              .noSpaces
              .getBytes("UTF-8")
        )
        val encodedPayload =
            base64urlEncoder.encodeToString(payload.asJson.noSpaces.getBytes("UTF-8"))
        val signingInput = s"$encodedHeader.$encodedPayload"
        val signature = base64urlEncoder.encodeToString(
          keyMaterial.sign(signingInput.getBytes("UTF-8"))
        )
        Ucan(header, payload, signingInput, signature)
    }

    def encodeJwt(ucan: Ucan): String = {
        s"${ucan.signedData}.${ucan.signature}"
    }

    def decodeJwt(token: String): Try[Ucan] = {
        Try {
            val parts = token.split("\\.")
            require(parts.length == 3, "Invalid JWT format: must have three parts")

            val Array(encodedHeader, encodedPayload, encodedSignature) = parts

            // header
            val headerJson =
                new String(base64urlDecoder.decode(encodedHeader), "UTF-8")
            val header = decode[Header](headerJson).getOrElse(
              throw new Exception("Invalid header format")
            )

            // payload
            val payloadJson =
                new String(base64urlDecoder.decode(encodedPayload), "UTF-8")
            val payload = decode[Payload](payloadJson).getOrElse(
              throw new Exception("Decoding failed")
            )

            // signed data
            val signedData = s"$encodedHeader.$encodedPayload"

            Ucan(header, payload, signedData, encodedSignature)
        }
    }

    def validateRevocation(revocation: RevocationMessage, resolver: ProofResolver): Try[Unit] = Try {
        val issuerKeyMaterial = didParser
            .parse(revocation.iss)
            .getOrElse(throw new SecurityException(s"Unknown issuer DID: ${revocation.iss}"))

        // verify the signature
        val message = s"REVOKE:${revocation.revoke}"
        val signatureBytes = base64urlDecoder.decode(revocation.challenge)

        if (!issuerKeyMaterial.verify(message.getBytes("UTF-8"), signatureBytes)) {
            throw new SecurityException("Invalid revocation signature")
        }

        val ucanCid = cidParser.parse(revocation.revoke).get
        val ucan = resolver.resolve(ucanCid).get

        // Check if the revocation issuer matches the UCAN issuer
        if (revocation.iss != ucan.payload.iss) {
            throw new SecurityException("Invalid revocation: Revocation issuer does not match UCAN issuer")
        }
    }

    def validateToken(
        token: String,
        resolver: ProofResolver = new InMemoryProofResolver()
    ): Try[List[Capability]] = {
        for {
            ucan <- decodeJwt(token)
            // basic validation (signature, time, etc.)
            _ <- validateBasicChecks(ucan, resolver)

            // validate capabilities and return the valid ones
            validCapabilities <- validateCapabilityChain(ucan, resolver)
        } yield validCapabilities
    }

    def validateBasicChecks(
        ucan: Ucan,
        resolver: ProofResolver
    ): Try[Unit] = {
        for {
            // Check if this UCAN is directly revoked
            _ <- Try {
                val cid = createCID(ucan)
                if (resolver.revocationStore.isRevoked(cid)) {
                    throw new SecurityException(s"UCAN has been revoked: cid=${cid.encode()}")
                }
            }
            // check cryptographic signature
            _ <- Try {
                val issuerDid = ucan.payload.iss
                val issuerKeyMaterial = didParser
                    .parse(issuerDid)
                    .getOrElse(
                      throw new SecurityException(s"Unknown issuer DID: $issuerDid")
                    )
                val audienceDid = ucan.payload.aud
                val audKeyMaterial = didParser
                    .parse(audienceDid)
                    .getOrElse(
                      throw new SecurityException(s"Unknown audience DID: $audienceDid")
                    )

                if (ucan.header.alg != issuerKeyMaterial.jwtAlg) {
                    throw new SecurityException(
                      s"Algorithm mismatch. Expected ${issuerKeyMaterial.jwtAlg}, got ${ucan.header.alg}"
                    )
                }

                val signatureBytes = base64urlDecoder.decode(ucan.signature)
                if (
                  !issuerKeyMaterial.verify(
                    ucan.signedData.getBytes("UTF-8"),
                    signatureBytes
                  )
                ) {
                    throw new SecurityException("Invalid cryptographic signature")
                }
            }
            // check time
            _ <- Try {
                val now = Instant.now()

                ucan.payload.nbf.foreach { nbf =>
                    if (now.isBefore(nbf))
                        throw new SecurityException(s"Token not valid before $nbf")
                }

                ucan.payload.exp match {
                    case Some(exp) if now.isAfter(exp) =>
                        throw new SecurityException(s"Token expired at $exp")
                    case None =>
                        throw new SecurityException("Missing required expiration claim (exp)")
                    case _ => // Valid
                }
            }
            // check version etc
            _ <- Try {
                if (ucan.payload.ucv != Version) {
                    throw new SecurityException(
                      s"Unsupported UCAN version: ${ucan.payload.ucv}"
                    )
                }

                if (ucan.payload.cap.isEmpty) {
                    throw new SecurityException("UCAN must contain at least one capability")
                }

                ucan.payload.prf.foreach { proofs =>
                    if (proofs.exists(_.version != 1)) {
                        throw new SecurityException("Invalid CID version in proofs")
                    }
                }
            }
            // validate capabilities structure
            _ <- Try {
                ucan.payload.cap.foreach { capability =>
                    // Check resource (valid URI)
                    Try(new java.net.URI(capability.resource)).getOrElse(
                      throw new SecurityException(
                        s"Invalid resource URI: ${capability.resource}"
                      )
                    )

                    // Check abilities
                    if (capability.abilities.isEmpty) {
                        throw new SecurityException(
                          "Capability must have at least one ability"
                        )
                    }
                }
            }
        } yield ()
    }

    def validateCapabilityChain(
        ucan: Ucan,
        resolver: ProofResolver
    ): Try[List[Capability]] = {
        if (ucan.payload.prf.isEmpty) {
            return Success(ucan.payload.cap);
        }

        val validProofs = prefilterProofs(ucan, resolver).getOrElse(List.empty)
        // Validate each capability and collect errors
        val (validCapabilities, errors) = ucan.payload.cap.foldLeft((List.empty[Capability], List.empty[String])) {
            case ((valid, errs), capability) =>
                validateCapabilityWithProofs(capability, validProofs, ucan.payload.iss, resolver) match {
                    case Success(validatedCapability) => (valid :+ validatedCapability, errs)
                    case Failure(e)                   => (valid, errs :+ e.getMessage)
                }
        }

        if (validCapabilities.isEmpty) {
            Failure(
              new SecurityException(s"[${ucan.payload.iss}] Could not prove any capabilities: ${errors.mkString("; ")}")
            )
        } else {
            Success(validCapabilities)
        }
    }

    def validateCapabilityWithProofs(
        capability: Capability,
        proofCIDs: List[CID],
        issuer: String,
        resolver: ProofResolver
    ): Try[Capability] = {
        // Check if proofs list is empty and handle that case first
        if (proofCIDs.isEmpty) {
            val noProofErrors = capability.abilities.keys.map { ability =>
                s"[$ability] No proofs provided"
            }.toList

            return Failure(
              new SecurityException(
                s"No valid proof found for capability: \"${capability.resource}\", Requested abilities: [${capability.abilities.keys
                        .mkString(", ")}]. Errors: ${noProofErrors.mkString("; ")}"
              )
            )
        }

        // Process each ability independently
        val abilityResults = capability.abilities.map { case (ability, caveats) =>
            val singleAbilityCapability = Capability(capability.resource, Map(ability -> caveats))

            // Try to validate this ability with any of the proofs
            val validationResults = proofCIDs.map { proofCID =>
                try {
                    val proofUcan = resolver.resolve(proofCID).get
                    val tokenValidated = validateToken(encodeJwt(proofUcan), resolver).isSuccess
                    if (!tokenValidated) {
                        Success(Left(s"Token validation failed for proof: ${proofCID.encode()}"))
                    } else if (proofUcan.payload.aud != issuer) {
                        Success(
                          Left(
                            s"Delegation chain broken: current issuer $issuer does not match proof audience ${proofUcan.payload.aud}"
                          )
                        )
                    } else {
                        val authorizations = proofUcan.payload.cap.map { proofCap =>
                            validateAttenuation(proofCap, singleAbilityCapability)
                        }

                        if (authorizations.exists(_.isRight)) {
                            Success(Right(true))
                        } else {
                            val errors = authorizations.collect { case Left(error) => error }
                            Success(Left(errors.mkString(", ")))
                        }
                    }
                } catch {
                    case e: Exception => Success(Left(e.getMessage))
                }
            }

            // check if any proof successfully validated this ability
            val isValid = validationResults.exists {
                case Success(Right(true)) => true
                case _                    => false
            }

            // collect all errors for this ability
            val errors = validationResults.collect { case Success(Left(errorMsg)) =>
                errorMsg
            }

            (ability, isValid, errors)
        }

        // collect all successfully validated abilities
        val validAbilities = abilityResults.collect { case (ability, true, _) =>
            (ability, capability.abilities(ability))
        }.toMap

        // collect errors for failed abilities
        val errorMessages = abilityResults.collect { case (ability, false, errors) =>
            s"[$ability] ${if (errors.isEmpty) "No valid proofs found" else errors.mkString("; ")}"
        }

        // return successful result with valid abilities
        if (validAbilities.isEmpty) {
            Failure(
              new SecurityException(
                s"No valid proof found for capability: \"${capability.resource}\", Requested abilities: [${capability.abilities.keys
                        .mkString(", ")}]. Errors: ${errorMessages.mkString("; ")}"
              )
            )
        } else {
            Success(Capability(capability.resource, validAbilities))
        }
    }

    // see table https://github.com/ucan-wg/spec/tree/v0.10.0?tab=readme-ov-file#3263-caveat-array
    def validateAttenuation(
        proofCap: Capability,
        delegatedCap: Capability
    ): Either[String, Boolean] = {
        // check if resource matches or is a sub-resource
        if (
          proofCap.resource != delegatedCap.resource && !delegatedCap.resource
              .startsWith(proofCap.resource)
        ) {
            return Left(
              s"Resource mismatch: delegated resource '${delegatedCap.resource}' is not a sub-resource of proof resource '${proofCap.resource}'"
            )
        }

        def normalizeAbility(ability: String): String = ability.toLowerCase

        val normalizedProofAbilities = proofCap.abilities.map { case (ability, caveats) =>
            normalizeAbility(ability) -> (ability, caveats)
        }.toMap

        val abilityResults = delegatedCap.abilities.map { case (rawAbility, delegatedCaveats) =>
            val ability = normalizeAbility(rawAbility)
            normalizedProofAbilities.get(ability) match {
                case None =>
                    Left(s"Ability '$rawAbility' not found in proof capabilities")
                case Some((originalAbility, proofCaveats)) =>
                    if (proofCaveats.isEmpty) {
                        Left(
                          s"Empty caveat array for ability '$originalAbility' means 'in no case' - no operations are authorized"
                        )
                    } else {
                        isValidAttenuation(proofCaveats, delegatedCaveats)
                    }
            }
        }

        // if all ability validations succeed, the attenuation is valid
        if (abilityResults.forall(_.isRight)) {
            Right(true)
        } else {
            // get all child error messages
            val errors = abilityResults.collect { case Left(error) => error }
            Left(
              s"Capability attenuation validation failed: ${errors.mkString("")}"
            )
        }
    }

    def isValidAttenuation(
        proofCaveats: List[Json],
        delegatedCaveats: List[Json]
    ): Either[String, Boolean] = {
        // For each delegated caveat, there must be at least one proof caveat that it attenuates
        val validations = delegatedCaveats.map { delegatedCaveat =>
            val isValid = proofCaveats.exists(proofCaveat => isCaveatAttenuated(proofCaveat, delegatedCaveat))
            if (!isValid) {
                Left(
                  s"Delegated caveat ${delegatedCaveat.noSpaces} is not a valid attenuation of any proof caveat"
                )
            } else {
                Right(true)
            }
        }

        // If all delegated caveats are valid attenuations, the overall attenuation is valid
        if (validations.forall(_.isRight)) {
            Right(true)
        } else {
            val errors = validations.collect { case Left(error) => error }
            Left(errors.mkString(", "))
        }
    }

    private def isCaveatAttenuated(
        proofCaveat: Json,
        delegatedCaveat: Json
    ): Boolean = {
        // if proof caveat is an empty object {}, any delegated caveat is valid
        if (proofCaveat.asObject.exists(_.isEmpty)) {
            return true
        }

        // if delegated caveat is an empty object {}, it's only valid if proof caveat is also empty (otherwise it's an escalation)
        if (delegatedCaveat.asObject.exists(_.isEmpty)) {
            return proofCaveat.asObject.exists(_.isEmpty)
        }

        (proofCaveat.asObject, delegatedCaveat.asObject) match {
            case (Some(proofObj), Some(delegatedObj)) =>
                // Check that all fields in proof caveat exist in delegated caveat with the same values (for existing keys the values may not be changed)
                // Any newly added fields in the delegated caveat are allowed (more restrictions)
                proofObj.toMap.forall { case (key, value) =>
                    delegatedObj.toMap.get(key).contains(value)
                }
            case _ => false
        }
    }

    def prefilterProofs(
        ucan: Ucan,
        resolver: ProofResolver
    ): Try[List[CID]] = {
        Try {
            ucan.payload.prf
                .map { proofs =>
                    proofs.filter { proofCID =>
                        // Skip revoked proofs
                        if (resolver.revocationStore.isRevoked(proofCID)) {
                            false
                        } else {
                            resolver.resolve(proofCID) match {
                                case Success(proofUcan) =>
                                    // Check time bounds are valid
                                    val validNbf =
                                        ucan.payload.nbf.forall(n => proofUcan.payload.nbf.forall(p => !n.isBefore(p)))

                                    // For exp: The UCAN's exp should not be later than proof's exp
                                    val validExp =
                                        ucan.payload.exp.forall(e => proofUcan.payload.exp.forall(p => !e.isAfter(p)))

                                    validNbf && validExp
                                case Failure(_) => false
                            }
                        }
                    }
                }
                .getOrElse(List.empty)
        }
    }

    def createRevocation(
        ucanCid: CID,
        issuerKeyMaterial: KeyMaterial
    ): Try[RevocationMessage] = Try {
        val issuerDid = didParser.keyMaterialToDid(issuerKeyMaterial)
        val canonicalCid = ucanCid.encode()
        val message = s"REVOKE:${canonicalCid}"
        val signature = base64urlEncoder.encodeToString(
          issuerKeyMaterial.sign(message.getBytes("UTF-8"))
        )

        RevocationMessage(
          iss = issuerDid,
          revoke = canonicalCid,
          challenge = signature
        )
    }

  //TODO: SHOULD THIS RETURN A FUTURE?
  def createDefaultKeymaterial(): KeyMaterial = scala.concurrent.Await.result(Ed25519KeyMaterial.create(), scala.concurrent.duration.Duration.Inf)

    def parseDefaultKeyMaterial(publicKey: Array[Byte], privateKey: Option[Array[Byte]] = None): Ed25519KeyMaterial = {
        Ed25519KeyMaterial(publicKey, privateKey)
    }

    def createDefaultDidParser(): DidParser = DidParser.create()

    def createDefaultCidParser(): CidParser = CidParser.create()
}
